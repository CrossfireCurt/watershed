package com.commercehub.watershed.pump.application.factories;

import com.commercehub.watershed.pump.model.Job;
import com.commercehub.watershed.pump.processing.Pump;
import com.commercehub.watershed.pump.processing.PumpSubscriber;

/**
 * Guice Factory to create PumpSubscribers (with dependency injection).
 */
public interface PumpSubscriberFactory {

    /**
     * Creates a new PumpSubscriber
     * @param job
     * @param pump
     * @return a new PumpSubscriber
     */
    PumpSubscriber create(Job job, Pump pump);
}
