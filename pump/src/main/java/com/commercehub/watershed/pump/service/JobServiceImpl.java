package com.commercehub.watershed.pump.service;

import com.commercehub.watershed.pump.application.factories.JobFactory;
import com.commercehub.watershed.pump.application.factories.JobRunnableFactory;
import com.commercehub.watershed.pump.model.Job;
import com.commercehub.watershed.pump.model.JobPreview;
import com.commercehub.watershed.pump.model.PreviewSettings;
import com.commercehub.watershed.pump.model.PumpSettings;
import com.commercehub.watershed.pump.repositories.QueryableRepository;
import com.google.inject.Inject;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Provides a handful of methods to manage Jobs.
 */
public class JobServiceImpl implements JobService {

    @Inject
    private Map<String, Job> jobMap;

    @Inject
    private JobRunnableFactory jobRunnableFactory;

    @Inject
    private ExecutorService executor;

    @Inject
    private QueryableRepository repository;

    @Inject
    private JobFactory jobFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public Job enqueueJob(PumpSettings pumpSettings) {
        Job job = jobFactory.create(UUID.randomUUID().toString(), pumpSettings);
        jobMap.put(job.getJobId(), job);

        executor.submit(jobRunnableFactory.create(job));
        return job;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Job getJob(String jobId) {
        return jobMap.get(jobId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Job> getAllJobs(){
        return jobMap.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobPreview getJobPreview(PreviewSettings previewSettings) throws SQLException {
        return repository.getJobPreview(previewSettings);
    }
}
