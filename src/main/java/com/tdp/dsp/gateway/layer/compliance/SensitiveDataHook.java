package com.tdp.dsp.gateway.layer.compliance;

import com.fasterxml.jackson.databind.JsonNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.model.common.Direction;
import com.tdp.dsp.gateway.model.common.InteropEnvelope;

/**
 * 数据分类分级出境控制（预留实现）。
 *
 * <p>只拦截出境。读取报文体或嵌套 {@code product} 上的 {@code classification}。
 * 「重要数据」「核心数据」默认 DENY（{@code CB-CLASSIFICATION}），需后续安全评估扩展点放行。
 * 「一般数据」或缺省分类放行，便于演示目录查询。
 *
 * <p>入境不检查：境外数据进入国内空间由国内平台侧策略约束，不在本钩子范围。
 */
public class SensitiveDataHook implements ComplianceHook {

    @Override
    public String name() {
        return "sensitive-data";
    }

    @Override
    public ComplianceDecision check(InteropEnvelope envelope) {
        if (envelope.direction() != Direction.OUTBOUND) {
            return ComplianceDecision.allow();
        }
        JsonNode payload = envelope.payload();
        String classification = Jsons.text(payload, "classification");
        if (classification == null) {
            JsonNode product = Jsons.get(payload, "product");
            classification = Jsons.text(product, "classification");
        }
        if ("重要数据".equals(classification) || "核心数据".equals(classification)) {
            return ComplianceDecision.deny(
                    "CB-CLASSIFICATION",
                    "数据分类为「" + classification + "」，跨境出口被合规关口拦截，待安全评估扩展点放行"
            );
        }
        return ComplianceDecision.allow();
    }
}
