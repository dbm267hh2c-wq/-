package com.tdp.dsp.gateway.model.common;

/**
 * 境内连接器与 IDS 参与方的双向身份映射。
 *
 * <p>同一法律主体在两边各有一套 ID。桥接层按 {@code tdpEntityId} /
 * {@code idsParticipantId} / {@code tdpConnectorId} 建索引，出境时用其中任一键解析对端。
 *
 * @param tdpEntityId       国内主体 ID
 * @param tdpConnectorId    国内连接器 ID
 * @param tdpConnectorName  国内连接器名称
 * @param idsParticipantId  IDS DID
 * @param idsConnectorUrl   IDS 连接器基址
 * @param region            区域，如 {@code CN} / {@code EU}
 * @param dataspace         {@code tdp} 表示本侧；其它值视为境外空间
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
    /** 默认国内参与方：区域 CN、空间 tdp。 */
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
