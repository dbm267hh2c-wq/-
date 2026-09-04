package com.tdp.dsp.gateway.layer.compliance;

/**
 * 单次合规裁决。
 *
 * @param allowed {@code true} 放行
 * @param code    稳定错误码，如 {@code CB-CLASSIFICATION}；放行为 {@code ALLOW}
 * @param message 可读说明，会进入审计与拒绝响应
 */
public record ComplianceDecision(boolean allowed, String code, String message) {

    public static ComplianceDecision allow() {
        return new ComplianceDecision(true, "ALLOW", "通过");
    }

    public static ComplianceDecision deny(String code, String message) {
        return new ComplianceDecision(false, code, message);
    }
}
