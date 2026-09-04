package com.tdp.dsp.gateway.layer.compliance;

import com.tdp.dsp.gateway.model.common.InteropEnvelope;

/**
 * 合规校验扩展点。后续跨境合规（数据分类分级、目的国评估、出境安全评估等）在此接入。
 */
public interface ComplianceHook {

    String name();

    ComplianceDecision check(InteropEnvelope envelope);
}
