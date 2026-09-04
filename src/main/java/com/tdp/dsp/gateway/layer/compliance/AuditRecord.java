package com.tdp.dsp.gateway.layer.compliance;

import com.tdp.dsp.gateway.model.common.Direction;

import java.time.Instant;

public record AuditRecord(
        String auditId,
        Instant timestamp,
        Direction direction,
        String sourceProtocol,
        String targetProtocol,
        String operation,
        String peer,
        String decision,
        String code,
        String message
) {
}
