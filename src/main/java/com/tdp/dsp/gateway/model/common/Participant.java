package com.tdp.dsp.gateway.model.common;

/**
 * 境内连接器与 IDS 参与方的双向身份映射。
 */
public record Participant(
        String tdpEntityId,
        String tdpConnectorId,
        String tdpConnectorName,
        String idsParticipantId,
        String idsConnectorUrl,
        String region,
        String dataspace
) {
    public Participant(
            String tdpEntityId,
            String tdpConnectorId,
            String tdpConnectorName,
            String idsParticipantId,
            String idsConnectorUrl
    ) {
        this(tdpEntityId, tdpConnectorId, tdpConnectorName, idsParticipantId, idsConnectorUrl, "CN", "tdp");
    }
}
