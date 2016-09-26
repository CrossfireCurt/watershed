package com.commercehub.watershed.pump.service;

import com.google.common.base.Function;

/**
 * Defines methods used to transform records.
 */
public interface TransformerFunctionFactory {

    /**
     * Function that adds Replay and Overwrite flags to a JSON object.
     * @param replayEnabled
     * @param overwriteEnabled
     * @return Function
     */
    Function<byte[], byte[]> getReplayFlagTransformFunction(Boolean replayEnabled, Boolean overwriteEnabled);
}
