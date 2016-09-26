package com.commercehub.watershed.pump.application.factories;

import com.commercehub.watershed.pump.model.PumpSettings;
import com.commercehub.watershed.pump.processing.Pump;
import com.google.common.base.Function;

/**
 * Guice Factory to create Pumps (with dependency injection).
 */
public interface PumpFactory {

    /**
     * Creates a new Pump
     * @param pumpSettings
     * @param recordTransformer
     * @return a new Pump
     */
    Pump create(PumpSettings pumpSettings, Function<byte[], byte[]> recordTransformer);
}
