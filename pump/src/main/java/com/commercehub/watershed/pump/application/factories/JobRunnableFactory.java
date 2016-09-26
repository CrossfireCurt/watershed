package com.commercehub.watershed.pump.application.factories;

import com.commercehub.watershed.pump.model.Job;
import com.commercehub.watershed.pump.processing.JobRunnable;

/**
 * Guice Factory to create JobRunnables (with dependency injection).
 */
public interface JobRunnableFactory {

    /**
     * Creates a new JobRunnable
     * @param job
     * @return a new JobRunnable
     */
    JobRunnable create(Job job);
}
