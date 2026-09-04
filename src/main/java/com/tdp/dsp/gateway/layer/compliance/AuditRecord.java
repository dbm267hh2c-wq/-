package com.tdp.dsp.gateway.layer.compliance;

import com.tdp.dsp.gateway.model.common.Direction;

import java.time.Instant;

/**
 * 一笔跨境流量的审计条目。当前存在进程内存，供 {@code GET /audit} 查询。
 *
 * @param auditId         条目 UUID
 * @param timestamp       裁决时间
 * @param direction       入境 / 出境
 * @param sourceProtocol  源协议
 * @param targetProtocol  目标协议
 * @param operation       操作码或 DSP 类型；响应审计带 {@code :response} 后缀
 * @param peer            对端 IP 或 IDS DID
 * @param decision        {@code ALLOW} / {@code DENY}
 * @param code            合规错误码
 * @param message         说明
 */
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
