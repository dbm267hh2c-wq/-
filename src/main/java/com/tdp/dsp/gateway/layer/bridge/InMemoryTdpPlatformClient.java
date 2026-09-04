package com.tdp.dsp.gateway.layer.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.model.common.Participant;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存模拟的国内可信数据空间服务平台。
 *
 * <p>预置两款产品：交通流量（一般数据，可出境演示）与电力负荷（重要数据，用于合规拦截演示）。
 * 合约状态机简化为：发起 → 协商 / 签署成功 → 履行中 → 终止。
 *
 * <p>合约编号按国标风格拼接（类型 + 主体 + 时间 + 流水），仅用于联调可读，非正式发号器。
 */
public class InMemoryTdpPlatformClient implements TdpPlatformClient {

    private final Participant localParticipant;
    private final Map<String, ObjectNode> products = new ConcurrentHashMap<>();
    private final Map<String, ObjectNode> contracts = new ConcurrentHashMap<>();

    public InMemoryTdpPlatformClient(Participant localParticipant) {
        this.localParticipant = localParticipant;
        seed();
    }

    private void seed() {
        ObjectNode traffic = product(
                "DP-CN-TRAFFIC-0001",
                "城市交通流量数据产品",
                "国内城市道路断面流量与拥堵指数",
                List.of("交通", "城市"),
                "CSV",
                "一般数据"
        );
        ObjectNode energy = product(
                "DP-CN-ENERGY-0002",
                "区域电力负荷数据产品",
                "省级电网日前负荷预测与实测",
                List.of("能源", "电力"),
                "JSON",
                "重要数据"
        );
        products.put(traffic.get("dataProductId").asText(), traffic);
        products.put(energy.get("dataProductId").asText(), energy);
    }

    private ObjectNode product(
            String id,
            String name,
            String abs,
            List<String> keywords,
            String format,
            String classification
    ) {
        ObjectNode product = Jsons.object();
        product.put("dataProductId", id);
        product.put("dataProductName", name);
        product.put("dataProductAbstract", abs);
        product.put("ownerEntityId", localParticipant.tdpEntityId());
        product.put("ownerName", "国内可信数据空间提供方");
        ArrayNode keywordNode = Jsons.array();
        keywords.forEach(keywordNode::add);
        product.set("keywords", keywordNode);
        product.put("format", format);
        product.put("endpointUrl", "https://tdp.example.cn/connector");
        product.put("connectorId", localParticipant.tdpConnectorId());
        product.put("classification", classification);
        ObjectNode strategy = Jsons.object();
        strategy.set("subjectInfo", Jsons.objectOf("dataProductId", id, "dataProductName", name));
        strategy.set("executionNodeInfo", Jsons.objectOf(
                "providerNodeId", localParticipant.tdpConnectorId(),
                "providerNodeName", localParticipant.tdpConnectorName()
        ));
        ArrayNode actions = Jsons.array();
        actions.add("授权使用");
        actions.add("读取");
        strategy.set("actions", actions);
        ArrayNode constraints = Jsons.array();
        constraints.add(Jsons.objectOf(
                "constraintName", "使用目的",
                "constraintOperator", "01",
                "constraintValue", "科研"
        ));
        constraints.add(Jsons.objectOf(
                "constraintName", "使用次数",
                "constraintOperator", "11",
                "constraintValue", "1000"
        ));
        strategy.set("constraints", constraints);
        product.set("strategy", strategy);
        return product;
    }

    @Override
    public ObjectNode catalogQuery(ObjectNode query) {
        String keyword = Jsons.text(query, "keyword");
        ArrayNode matched = Jsons.array();
        for (ObjectNode product : products.values()) {
            if (keyword == null || keyword.isBlank() || containsKeyword(product, keyword)) {
                matched.add(product.deepCopy());
            }
        }
        ObjectNode response = Jsons.object();
        response.put("status", "0");
        response.put("total", matched.size());
        response.set("products", matched);
        return response;
    }

    @Override
    public ObjectNode productDetail(ObjectNode query) {
        String id = Jsons.textOrEmpty(query, "dataProductId");
        ObjectNode product = products.get(id);
        ObjectNode response = Jsons.object();
        if (product == null) {
            response.put("status", "1");
            response.put("message", "未找到数据产品: " + id);
            return response;
        }
        response.put("status", "0");
        response.set("product", product.deepCopy());
        return response;
    }

    @Override
    public ObjectNode contractCreate(ObjectNode request) {
        String contractId = newContractId();
        ObjectNode stored = request.deepCopy();
        stored.put("contractId", contractId);
        stored.put("contractStatus", "发起");
        contracts.put(contractId, stored);
        ObjectNode response = Jsons.object();
        response.put("contractId", contractId);
        response.put("creationFlag", "0");
        return response;
    }

    @Override
    public ObjectNode contractNegotiate(ObjectNode request) {
        String contractId = Jsons.textOrEmpty(request, "contractId");
        ObjectNode stored = contracts.computeIfAbsent(contractId, key -> request.deepCopy());
        stored.set("strategy", request.get("strategy"));
        stored.put("contractStatus", "协商");
        if (request.get("signatureList") != null && request.get("signatureList").size() > 0) {
            stored.put("contractStatus", "签署成功");
        }
        ObjectNode response = Jsons.object();
        response.put("negotiationStatus", "0");
        response.put("contractId", contractId);
        return response;
    }

    @Override
    public ObjectNode contractExecution(ObjectNode request) {
        String contractId = Jsons.textOrEmpty(request, "contractId");
        ObjectNode stored = contracts.computeIfAbsent(contractId, key -> request.deepCopy());
        stored.put("contractStatus", "履行中");
        String transferId = "TR-" + UUID.randomUUID();
        ObjectNode response = Jsons.object();
        response.put("status", "0");
        response.put("contractId", contractId);
        response.put("transferId", transferId);
        response.put("endpointUrl", "https://tdp.example.cn/delivery/" + transferId);
        return response;
    }

    @Override
    public ObjectNode contractTerminate(ObjectNode request) {
        String contractId = Jsons.textOrEmpty(request, "contractId");
        ObjectNode stored = contracts.get(contractId);
        if (stored != null) {
            stored.put("contractStatus", "终止");
        }
        ObjectNode response = Jsons.object();
        response.put("status", "0");
        response.put("contractId", contractId);
        return response;
    }

    @Override
    public Participant localParticipant() {
        return localParticipant;
    }

    public Map<String, ObjectNode> products() {
        return Map.copyOf(products);
    }

    private boolean containsKeyword(ObjectNode product, String keyword) {
        String name = Jsons.textOrEmpty(product, "dataProductName");
        String abs = Jsons.textOrEmpty(product, "dataProductAbstract");
        JsonNode keywords = product.get("keywords");
        if (name.contains(keyword) || abs.contains(keyword)) {
            return true;
        }
        return Jsons.stringList(keywords).stream().anyMatch(item -> item.contains(keyword));
    }

    /**
     * 演示用合约编号：固定前缀 + 主体位 + 时间戳 + 流水。非正式国标发号实现。
     */
    private String newContractId() {
        String time = Instant.now().toString().replaceAll("[^0-9]", "");
        if (time.length() > 14) {
            time = time.substring(0, 14);
        }
        return "1" + "1" + pad(localParticipant.tdpEntityId(), 18) + "0001" + time + "12345678" + "0";
    }

    private static String pad(String value, int length) {
        String digits = value.replaceAll("[^0-9A-Za-z]", "");
        if (digits.length() >= length) {
            return digits.substring(0, length);
        }
        return "0".repeat(length - digits.length()) + digits;
    }
}
