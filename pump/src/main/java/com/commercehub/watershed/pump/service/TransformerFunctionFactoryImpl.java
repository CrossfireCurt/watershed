package com.commercehub.watershed.pump.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

/**
 * Provides Functions to transform records.
 */
public class TransformerFunctionFactoryImpl implements TransformerFunctionFactory {
    private static final Logger log = LoggerFactory.getLogger(TransformerFunctionFactoryImpl.class);

    @Inject
    private ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    public Function<byte[], byte[]> getReplayFlagTransformFunction(Boolean replayEnabled, Boolean overwriteEnabled) {
        final BooleanNode replayEnabledNode = replayEnabled? BooleanNode.TRUE : BooleanNode.FALSE;
        final BooleanNode overwriteEnabledNode = overwriteEnabled? BooleanNode.TRUE : BooleanNode.FALSE;

        return new Function<byte[], byte[]>() {
            @Override
            public byte[] apply(byte[] input) {
                try {
                    JsonNode tree = objectMapper.readTree(input);
                    if (JsonNodeType.OBJECT == tree.getNodeType()) {
                        ObjectNode rootObject = (ObjectNode) tree;
                        rootObject.set("replay", replayEnabledNode);
                        rootObject.set("overwrite", overwriteEnabledNode);
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream(input.length + 50);
                    objectMapper.writeValue(output, tree);
                    output.close();
                    return output.toByteArray();
                } catch (Exception e) {
                    log.warn("Failed to add replay flags to record, using original record", e);
                    return input;
                }
            }
        };
    }
}
