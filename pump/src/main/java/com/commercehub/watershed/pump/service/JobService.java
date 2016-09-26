package com.commercehub.watershed.pump.service;

import com.commercehub.watershed.pump.model.Job;
import com.commercehub.watershed.pump.model.JobPreview;
import com.commercehub.watershed.pump.model.PreviewSettings;
import com.commercehub.watershed.pump.model.PumpSettings;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Defines which methods we need in the JobService.
 */
public interface JobService {

    /**
     * Enqueue a Job for processing
     * @param pumpSettings
     * @return Job
     */
    Job enqueueJob(PumpSettings pumpSettings);

    /**
     * Retrieve a Job from the in-memory Job map
     * @param jobId
     * @return Job
     */
    Job getJob(String jobId);

    /**
     * Retrieve all Jobs from the in-memory Job map
     * @return a list of Jobs
     */
    Collection<Job> getAllJobs();

    /**
     * Submits a query for record count and a few preview records
     * @param previewSettings
     * @return JobPreview
     * @throws SQLException
     */
    JobPreview getJobPreview(PreviewSettings previewSettings) throws SQLException;
}
