package com.commercehub.watershed.pump.model;

import com.commercehub.watershed.pump.service.TimeService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import rx.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of all things related to the processing of a Pump run (statistics, setup, etc).
 */
public class Job {
    private TimeService timeService;

    private String jobId;
    private PumpSettings pumpSettings;
    private List<Throwable> processingErrors;
    private Subscription pumpSubscription;

    private Long successfulRecordCount;
    private Long failureRecordCount;
    private Long pendingRecordCount;

    private DateTime startTime;
    private DateTime completionTime;

    private ProcessingStage stage = ProcessingStage.NOT_STARTED;

    private DrillResultRow lastSuccessfulRow;

    private PeriodFormatter formatter = new PeriodFormatterBuilder()
            .printZeroNever().appendHours().appendSuffix(" hour, ", " hours, ")
            .printZeroNever().appendMinutes().appendSuffix(" minute, ", " minutes, ")
            .printZeroAlways().appendSeconds().appendSuffix(".")
            .printZeroAlways().appendMillis3Digit().appendSuffix(" seconds")
            .toFormatter();
            
    /**
     * Constructs a job with a unique ID and the PumpSettings associated with the job.
     * @param timeService
     * @param jobId
     * @param pumpSettings
     */
    @Inject
    public Job(
            TimeService timeService,
            @Assisted String jobId,
            @Assisted PumpSettings pumpSettings){
        this.timeService = timeService;
        this.jobId = jobId;
        this.pumpSettings = pumpSettings;
        this.processingErrors = new ArrayList<>();
    }

    /**
     *
     * @return jobId of the Job
     */
    public String getJobId() {
        return jobId;
    }

    /**
     *
     * @return pumpSettings of the Job
     */
    public PumpSettings getPumpSettings() {
        return pumpSettings;
    }

    /**
     *
     * @return stage at which job is processing
     */
    public ProcessingStage getStage() {
        return stage;
    }

    /**
     * Set the processing stage of the Job
     * @param stage
     */
    public void setStage(ProcessingStage stage) {
        this.stage = stage;
    }

    /**
     *
     * @return all the errors that happened during job processing
     */
    public List<Throwable> getProcessingErrors() {
        return processingErrors;
    }

    /**
     * add error that happened during job processing
     * @param processingError
     */
    public void addProcessingError(Throwable processingError) {
        processingErrors.add(processingError);
    }

    /**
     *
     * @return the pumpSubscription subscribed to the Pump Observable
     */
    public Subscription getPumpSubscription() {
        return pumpSubscription;
    }

    /**
     * set the pumpSubscription subscribed to the Pump Observable
     * @param pumpSubscription
     */
    public void setPumpSubscription(Subscription pumpSubscription) {
        this.pumpSubscription = pumpSubscription;
    }

    /**
     *
     * @return the number of successful records during job processing
     */
    public Long getSuccessfulRecordCount() {
        return successfulRecordCount;
    }

    /**
     * set the number of successful records during job processing
     * @param successfulRecordCount
     */
    public void setSuccessfulRecordCount(Long successfulRecordCount) {
        this.successfulRecordCount = successfulRecordCount;
    }

    /**
     *
     * @return the number of failed records during job processing
     */
    public Long getFailureRecordCount() {
        return failureRecordCount;
    }

    /**
     * set the number of failed records during job processing
     * @param failureRecordCount
     */
    public void setFailureRecordCount(Long failureRecordCount) {
        this.failureRecordCount = failureRecordCount;
    }

    /**
     *
     * @return the start time that Pump started processing the job
     */
    public DateTime getStartTime() {
        return startTime;
    }

    /**
     * set the start time for when the job started
     * @param startTime
     */
    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    /**
     *
     * @return completion time that Pump completed job processing
     */
    public DateTime getCompletionTime() {
        return completionTime;
    }

    /**
     * set the completion time for when the job completed
     * @param completionTime
     */
    public void setCompletionTime(DateTime completionTime) {
        this.completionTime = completionTime;
    }

    /**
     *
     * @return elapsed time between now and when the job started, or if CompletionTime is set, the time between the completion time and starting time
     */
    public Long getElapsedTime() {
        if(completionTime == null){
            return startTime != null? timeService.currentTimeMillis() - startTime.getMillis() : 0L;
        }

        return completionTime.getMillis() - startTime.getMillis();
    }

    /**
     *
     * @return a formatted string describing the elapsed time
     */
    public String getElapsedTimePretty() {
        return formatter.print(new Period(getElapsedTime() > 0? getElapsedTime().longValue() : 0L));
    }

    /**
     *
     * @return the number of records still pending for the job
     */
    public Long getPendingRecordCount() {
        return pendingRecordCount;
    }

    /**
     * set the number of records still pending for the job
     * @param pendingRecordCount
     */
    public void setPendingRecordCount(Long pendingRecordCount) {
        this.pendingRecordCount = pendingRecordCount;
    }

    /**
     *
     * @return the mean rate at which records are processing
     */
    public Double getMeanRate() {
        return getElapsedTime() > 0? (successfulRecordCount + failureRecordCount) / (getElapsedTime() / 1000d) : null;
    }

    /**
     *
     * @return a formatted string describing the mean rate
     */
    public String getMeanRatePretty() {
        return getElapsedTime() > 0 && getMeanRate() != null? String.format("%.1f rec/s", getMeanRate()) : "--- rec/s";
    }

    public DrillResultRow getLastSuccessfulRow() {
        return lastSuccessfulRow;
    }

    public void setLastSuccessfulRow(DrillResultRow lastSuccessfulRow) {
        this.lastSuccessfulRow = lastSuccessfulRow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Job job = (Job) o;

        if (!jobId.equals(job.jobId)) return false;
        if (!pumpSettings.equals(job.pumpSettings)) return false;
        if (processingErrors != null ? !processingErrors.equals(job.processingErrors) : job.processingErrors != null)
            return false;
        if (pumpSubscription != null ? !pumpSubscription.equals(job.pumpSubscription) : job.pumpSubscription != null)
            return false;
        if (lastSuccessfulRow != null ? !lastSuccessfulRow.equals(job.lastSuccessfulRow) : job.lastSuccessfulRow != null)
            return false;

        return stage == job.stage;
    }

    @Override
    public int hashCode() {
        int result = jobId.hashCode();
        result = 31 * result + pumpSettings.hashCode();
        result = 31 * result + (processingErrors != null ? processingErrors.hashCode() : 0);
        result = 31 * result + (pumpSubscription != null ? pumpSubscription.hashCode() : 0);
        result = 31 * result + (lastSuccessfulRow != null ? lastSuccessfulRow.hashCode() : 0);
        result = 31 * result + stage.hashCode();
        return result;
    }
}
