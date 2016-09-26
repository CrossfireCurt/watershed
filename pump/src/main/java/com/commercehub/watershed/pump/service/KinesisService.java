package com.commercehub.watershed.pump.service;

/**
 * Defines methods used to talk to Kinesis.
 */
public interface KinesisService {

    /**
     * Sends an API call to retrieve the number of shards on a Kinesis stream.
     * @param stream
     * @return numer of shards on a Kinesis stream.
     */
    int countShardsInStream(String stream);
}
