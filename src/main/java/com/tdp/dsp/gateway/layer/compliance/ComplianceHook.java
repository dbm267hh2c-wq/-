package com.tdp.dsp.gateway.layer.compliance;

import com.tdp.dsp.gateway.model.common.InteropEnvelope;

/**
 * 合规校验扩展点。
 *
 * <p>后续可在此接入：数据分类分级、目的国评估、出境安全评估、合同备案校验等。
 * 实现类应只读信封、返回 {@link ComplianceDecision}，不要直接调桥接或改 payload。
 */
public interface ComplianceHook {

    /** 稳定短名，出现在 {@code GET /compliance/extensions} 与日志中。 */
    String name();

    /**
     * @return ALLOW 继续下一个钩子；DENY 立即拦截整笔跨境请求
     */
    ComplianceDecision check(InteropEnvelope envelope);
}
