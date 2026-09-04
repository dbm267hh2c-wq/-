package com.tdp.dsp.gateway.model.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 跨层传递的统一信封。
 */
public record InteropEnvelope(
        Direction direction,
        String sourceProtocol,
        String targetProtocol,
        String operation,
        ObjectNode payload,
        ObjectNode metadata
) {
    public InteropEnvelope withPayload(ObjectNode newPayload) {
        return new InteropEnvelope(direction, sourceProtocol, targetProtocol, operation, newPayload, metadata);
    }

    public InteropEnvelope withOperation(String newOperation) {
        return new InteropEnvelope(direction, sourceProtocol, targetProtocol, newOperation, payload, metadata);
    }

    public JsonNode metadataValue(String field) {
        return metadata == null ? null : metadata.get(field);
    }
}
