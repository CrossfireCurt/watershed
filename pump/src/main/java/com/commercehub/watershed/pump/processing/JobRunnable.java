package com.commercehub.watershed.pump.processing;

import com.commercehub.watershed.pump.application.factories.PumpFactory;
import com.commercehub.watershed.pump.application.factories.PumpSubscriberFactory;
import com.commercehub.watershed.pump.model.Job;
import com.commercehub.watershed.pump.model.ProcessingStage;
import com.commercehub.watershed.pump.model.PumpRecordResult;
import com.commercehub.watershed.pump.model.PumpSettings;
import com.commercehub.watershed.pump.service.TransformerFunctionFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

/**
 * Kicks off a Pump instance to run a Job.
 */
public class JobRunnable implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(JobRunnable.class);

    private Job job;
    private TransformerFunctionFactory transformerFunctionFactory;

    private PumpFactory pumpFactory;
    private PumpSubscriberFactory pumpSubscriberFactory;

    @Inject
    public JobRunnable(
            TransformerFunctionFactory transformerFunctionFactory,
            PumpFactory pumpFactory,
            PumpSubscriberFactory pumpSubscriberFactory,
            @Assisted Job job){

        this.transformerFunctionFactory = transformerFunctionFactory;
        this.pumpFactory = pumpFactory;
        this.pumpSubscriberFactory = pumpSubscriberFactory;
        this.job = job;
    }

    /**
     * Run Pump
     */
    public void run(){
        if(job == null){
            throw new IllegalStateException("Job cannot be null.");
        }

        if(job.getStage() != ProcessingStage.NOT_STARTED){
            throw new IllegalStateException("Job already running or completed.");
        }

        PumpSettings pumpSettings = job.getPumpSettings();
        final Pump pump = pumpFactory.create(
                pumpSettings,
                transformerFunctionFactory.getReplayFlagTransformFunction(pumpSettings.getHasReplayFlag(), pumpSettings.getHasOverwriteFlag()));

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("Destroying pump (job: {})", job.getJobId());
                pump.destroy();
            }
        }, "KPL shutdown hook"));

        Observable<PumpRecordResult> results = pump.build();
        Subscription subscription = results.subscribe(pumpSubscriberFactory.create(job, pump));
        job.setPumpSubscription(subscription);
    }
}
