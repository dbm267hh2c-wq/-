package com.tdp.dsp.gateway.model.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 跨层传递的统一信封。四层只认信封，不直接读 HttpServletRequest。
 *
 * @param direction       入境或出境，决定合规策略与响应形态
 * @param sourceProtocol  源协议名，审计用（{@code DSP} / {@code TDP}）
 * @param targetProtocol  目标协议名
 * @param operation       入境为 DSP {@code @type} 短名，出境为国内操作码
 * @param payload         原始业务报文（尚未转换）
 * @param metadata        对端身份、目的国、peer IP 等控制面信息
 */
public record InteropEnvelope(
        Direction direction,
        String sourceProtocol,
        String targetProtocol,
        String operation,
        ObjectNode payload,
        ObjectNode metadata
) {
    /** 替换业务报文体，方向与操作保持不变。 */
    public InteropEnvelope withPayload(ObjectNode newPayload) {
        return new InteropEnvelope(direction, sourceProtocol, targetProtocol, operation, newPayload, metadata);
    }

    /**
     * 仅改操作名。合规关口在响应审计时追加 {@code :response}，避免与请求审计条目混淆。
     */
    public InteropEnvelope withOperation(String newOperation) {
        return new InteropEnvelope(direction, sourceProtocol, targetProtocol, newOperation, payload, metadata);
    }

    public JsonNode metadataValue(String field) {
        return metadata == null ? null : metadata.get(field);
    }
}
