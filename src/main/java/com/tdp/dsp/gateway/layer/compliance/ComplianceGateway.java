package com.tdp.dsp.gateway.layer.compliance;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.layer.bridge.BridgeLayer;
import com.tdp.dsp.gateway.model.common.Direction;
import com.tdp.dsp.gateway.model.common.InteropEnvelope;
import com.tdp.dsp.gateway.model.dsp.DspMessages;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ④ 合规关口：跨境流量的唯一出入口。
 *
 * <p>处理顺序：注册钩子短路评估 → 写审计 → 拒绝则按方向返回错误形态，放行则交给桥接层，
 * 成功响应再记一条 {@code operation:response} 审计。
 *
 * <p>后续出境安全评估、目的国评估、合同备案等应实现 {@link ComplianceHook} 并 {@link #registerHook}，
 * 不要在 Controller 或桥接层里另开旁路。
 */
public class ComplianceGateway {

    private final BridgeLayer bridgeLayer;
    private final List<ComplianceHook> hooks = new CopyOnWriteArrayList<>();
    private final List<AuditRecord> auditLog = new CopyOnWriteArrayList<>();

    public ComplianceGateway(BridgeLayer bridgeLayer) {
        this.bridgeLayer = bridgeLayer;
    }

    public void registerHook(ComplianceHook hook) {
        hooks.add(hook);
    }

    public List<ComplianceHook> hooks() {
        return List.copyOf(hooks);
    }

    /**
     * 跨境请求总入口。无论放行还是拒绝都会落审计。
     */
    public ObjectNode handle(InteropEnvelope envelope) {
        ComplianceDecision decision = evaluate(envelope);
        audit(envelope, decision);
        if (!decision.allowed()) {
            return reject(envelope, decision);
        }
        ObjectNode response = bridgeLayer.dispatch(envelope);
        audit(envelope.withOperation(envelope.operation() + ":response"), ComplianceDecision.allow());
        return response;
    }

    /**
     * 按注册顺序执行钩子，第一个 DENY 立即返回；全部通过则 ALLOW。
     */
    public ComplianceDecision evaluate(InteropEnvelope envelope) {
        for (ComplianceHook hook : hooks) {
            ComplianceDecision decision = hook.check(envelope);
            if (!decision.allowed()) {
                return decision;
            }
        }
        return ComplianceDecision.allow();
    }

    public List<AuditRecord> auditLog() {
        return List.copyOf(auditLog);
    }

    public ObjectNode auditView() {
        ArrayNode items = Jsons.array();
        for (AuditRecord record : auditLog) {
            items.add(Jsons.objectOf(
                    "auditId", record.auditId(),
                    "timestamp", record.timestamp().toString(),
                    "direction", record.direction().name(),
                    "sourceProtocol", record.sourceProtocol(),
                    "targetProtocol", record.targetProtocol(),
                    "operation", record.operation(),
                    "peer", record.peer(),
                    "decision", record.decision(),
                    "code", record.code(),
                    "message", record.message()
            ));
        }
        return Jsons.objectOf("total", items.size(), "records", items);
    }

    private void audit(InteropEnvelope envelope, ComplianceDecision decision) {
        String peer = Jsons.textOrEmpty(envelope.metadata(), "peer", "idsParticipantId", "tdpConnectorId");
        auditLog.add(new AuditRecord(
                UUID.randomUUID().toString(),
                Instant.now(),
                envelope.direction(),
                envelope.sourceProtocol(),
                envelope.targetProtocol(),
                envelope.operation(),
                peer,
                decision.allowed() ? "ALLOW" : "DENY",
                decision.code(),
                decision.message()
        ));
    }

    /**
     * 拒绝响应与方向对齐：入境回 DSP {@code CatalogError}，出境回国内 {@code status=1}。
     */
    private ObjectNode reject(InteropEnvelope envelope, ComplianceDecision decision) {
        if (envelope.direction() == Direction.INBOUND) {
            return DspMessages.catalogError(decision.code(), decision.message());
        }
        ObjectNode response = Jsons.object();
        response.put("status", "1");
        response.put("code", decision.code());
        response.put("message", decision.message());
        return response;
    }

    public List<String> extensionPoints() {
        List<String> points = new ArrayList<>();
        points.add("ComplianceHook.check — 通用跨境合规校验");
        points.add("SensitiveDataHook — 数据分类分级出境控制");
        points.add("DestinationAllowlistHook — 目的国/目的空间白名单");
        points.add("AuditRecord — 跨境流量统一审计");
        return points;
    }
}
