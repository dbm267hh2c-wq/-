package com.tdp.dsp.gateway.layer.compliance;

import com.fasterxml.jackson.databind.JsonNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.model.common.Direction;
import com.tdp.dsp.gateway.model.common.InteropEnvelope;

/**
 * 预留扩展：重要数据 / 核心数据默认不允许直接出境。
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
