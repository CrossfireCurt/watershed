package com.commercehub.watershed.pump.service;


import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.StreamDescription;
import com.google.inject.Inject;

/**
 * Methods to interact with Kinesis.
 */
public class KinesisServiceImpl implements KinesisService {
    @Inject
    AmazonKinesisClient kinesisClient;

    /**
    * {@inheritDoc}
    */
    public int countShardsInStream(String stream) {
        int numShards = 0;
        StreamDescription desc = kinesisClient.describeStream(stream).getStreamDescription();
        int numShardsDescribed = desc.getShards().size();
        numShards += numShardsDescribed;
        while (desc.isHasMoreShards()) {
            desc = kinesisClient.describeStream(stream, desc.getShards().get(numShardsDescribed - 1).getShardId()).getStreamDescription();
            numShards += desc.getShards().size();
        }
        return numShards;
    }

}
