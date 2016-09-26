package com.commercehub.watershed.pump.application.factories;

import com.commercehub.watershed.pump.model.Job;
import com.commercehub.watershed.pump.model.PumpSettings;

/**
 * Guice Factory to create Jobs (with dependency injection).
 */
public interface JobFactory {

    /**
     * Creates a new Job
     * @param jobId
     * @param pumpSettings
     * @return a new Job
     */
    Job create(String jobId, PumpSettings pumpSettings);
}
