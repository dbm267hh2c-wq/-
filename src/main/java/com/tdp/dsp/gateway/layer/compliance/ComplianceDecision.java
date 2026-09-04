package com.tdp.dsp.gateway.layer.compliance;

public record ComplianceDecision(boolean allowed, String code, String message) {

    public static ComplianceDecision allow() {
        return new ComplianceDecision(true, "ALLOW", "通过");
    }

    public static ComplianceDecision deny(String code, String message) {
        return new ComplianceDecision(false, code, message);
    }
}
