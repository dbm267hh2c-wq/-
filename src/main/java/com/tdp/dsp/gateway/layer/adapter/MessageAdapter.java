package com.tdp.dsp.gateway.layer.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.mapping.FieldPath;
import com.tdp.dsp.gateway.mapping.MappingRegistry;

import java.util.List;
import java.util.UUID;

/**
 * ② 消息适配层：字段映射与语义转换。
 *
 * <p>叶子字段名、码值一律来自 {@link MappingRegistry}，不在本类写死。
 * 本类只处理「结构」：产品 ↔ Dataset、策略 ↔ ODRL Offer、目录过滤条件拼装。
 *
 * <p>ODRL 的 permission 是一对多（多个 action，每个可带一组 constraint），
 * 无法用单条点分路径表达，因此策略转换保留在 Java，约束叶子仍走码表 codec。
 */
public class MessageAdapter {

    private final MappingRegistry mappings;

    public MessageAdapter() {
        this(MappingRegistry.fromClasspath());
    }

    public MessageAdapter(MappingRegistry mappings) {
        this.mappings = mappings;
    }

    public MappingRegistry mappings() {
        return mappings;
    }

    /**
     * 国内数据产品 → DCAT Dataset。策略展开为 {@code odrl:hasPolicy} 数组（当前一条 Offer）。
     */
    public ObjectNode productToDataset(JsonNode product) {
        ObjectNode dataset = mappings.template("dataset");
        mappings.applyTdpToDsp("productToDataset", product, dataset);
        JsonNode strategy = Jsons.get(product, "strategy");
        ArrayNode policies = Jsons.array();
        policies.add(strategyToOffer(
                strategy,
                Jsons.textOrEmpty(product, "dataProductId"),
                Jsons.textOrEmpty(product, "ownerEntityId")
        ));
        dataset.set("odrl:hasPolicy", policies);
        return dataset;
    }

    /**
     * DCAT Dataset → 国内数据产品。
     * 摘要优先走字段绑定；若国内字段为空则回退 {@code dct:description[0]}（DSP 常用语言标签对象或纯文本）。
     */
    public ObjectNode datasetToProduct(JsonNode dataset) {
        ObjectNode product = mappings.template("product");
        mappings.applyDspToTdp("datasetToProduct", dataset, product);
        JsonNode description = FieldPath.read(dataset, "dct:description.0");
        if ((product.path("dataProductAbstract").asText("").isBlank()) && description != null && description.isTextual()) {
            product.put("dataProductAbstract", description.asText());
        }
        JsonNode policy = FieldPath.read(dataset, "odrl:hasPolicy.0");
        if (policy != null) {
            product.set("strategy", offerToStrategy(policy));
        }
        return product;
    }

    /**
     * 国内使用策略 → ODRL Offer。
     * 无 action 时用码表默认国内动作，保证 Offer 至少有一条 permission。
     */
    public ObjectNode strategyToOffer(JsonNode strategy, String targetId, String assigner) {
        ObjectNode offer = mappings.template("offer");
        offer.put("@id", "urn:uuid:" + UUID.nameUUIDFromBytes((targetId + "-offer").getBytes()));
        offer.put("odrl:assigner", assigner == null ? "" : assigner);
        offer.put("odrl:target", targetId);
        ArrayNode permissions = Jsons.array();
        List<String> actions = Jsons.stringList(Jsons.get(strategy, "actions"));
        if (actions.isEmpty()) {
            actions = List.of(mappings.actions().getDefaultTdp());
        }
        JsonNode rawConstraints = Jsons.get(strategy, "constraints");
        for (String action : actions) {
            ObjectNode permission = Jsons.object();
            permission.put("odrl:action", mapActionToOdrl(action));
            ArrayNode constraints = Jsons.array();
            if (rawConstraints != null && rawConstraints.isArray()) {
                rawConstraints.forEach(item -> constraints.add(constraintToOdrl(item)));
            }
            if (!constraints.isEmpty()) {
                permission.set("odrl:constraint", constraints);
            }
            permissions.add(permission);
        }
        offer.set("odrl:permission", permissions);
        return offer;
    }

    /**
     * ODRL Offer / Agreement → 国内策略。多个 permission 的 action 与 constraint 分别展平进两个数组。
     */
    public ObjectNode offerToStrategy(JsonNode offer) {
        ObjectNode strategy = mappings.template("strategy");
        JsonNode existingSubject = strategy.get("subjectInfo");
        ObjectNode subject = existingSubject instanceof ObjectNode objectNode ? objectNode : Jsons.object();
        strategy.set("subjectInfo", subject);
        subject.put("dataProductId", Jsons.textOrEmpty(offer, "odrl:target", "target"));
        ArrayNode actions = Jsons.array();
        ArrayNode constraints = Jsons.array();
        JsonNode permissions = Jsons.get(offer, "odrl:permission", "permission");
        if (permissions != null) {
            for (JsonNode permission : iterable(permissions)) {
                actions.add(mapActionToTdp(Jsons.textOrEmpty(permission, "odrl:action", "action")));
                JsonNode rawConstraints = Jsons.get(permission, "odrl:constraint", "constraint");
                if (rawConstraints != null) {
                    for (JsonNode constraint : iterable(rawConstraints)) {
                        constraints.add(odrlToConstraint(constraint));
                    }
                }
            }
        }
        strategy.set("actions", actions);
        strategy.set("constraints", constraints);
        return strategy;
    }

    /**
     * 国内目录查询 → DSP 过滤字段。keyword 与 filter 数组合并去重后写入 {@code dspace:filter}。
     */
    public ObjectNode catalogQueryToDspFilter(JsonNode catalogQuery) {
        ObjectNode request = mappings.applyTdpToDsp("catalogQueryToDsp", catalogQuery, Jsons.object());
        ArrayNode filters = Jsons.array();
        String keyword = Jsons.text(catalogQuery, "keyword");
        if (keyword != null && !keyword.isBlank()) {
            filters.add(keyword);
        }
        JsonNode existing = Jsons.get(catalogQuery, "filter");
        if (existing != null) {
            Jsons.stringList(existing).forEach(item -> {
                if (!containsText(filters, item)) {
                    filters.add(item);
                }
            });
        }
        if (!filters.isEmpty()) {
            request.set("dspace:filter", filters);
        }
        return request;
    }

    /**
     * DSP CatalogRequest → 国内目录查询。filter 首元素回填 {@code keyword}，整表保留在 {@code filter}。
     */
    public ObjectNode dspFilterToCatalogQuery(JsonNode catalogRequest, String issuerId, String issuerEntityId) {
        ObjectNode query = mappings.template("catalogQuery");
        mappings.applyDspToTdp("dspToCatalogQuery", catalogRequest, query);
        query.put("issuerId", issuerId);
        query.put("issuerEntityId", issuerEntityId);
        List<String> filters = Jsons.stringList(Jsons.get(catalogRequest, "dspace:filter", "filter"));
        if (!filters.isEmpty()) {
            query.put("keyword", filters.get(0));
            ArrayNode filterNode = Jsons.array();
            filters.forEach(filterNode::add);
            query.set("filter", filterNode);
        }
        return query;
    }

    public String mapActionToOdrl(String action) {
        return mappings.actionToDsp(action);
    }

    public String mapActionToTdp(String action) {
        return mappings.actionToTdp(action);
    }

    public String mapOperatorToOdrl(String operator) {
        return mappings.operatorToDsp(operator);
    }

    public String mapOperatorToTdp(String operator) {
        return mappings.operatorToTdp(operator);
    }

    public String mapConstraintNameToOdrl(String name) {
        return mappings.constraintToDsp(name);
    }

    public String mapConstraintNameToTdp(String name) {
        return mappings.constraintToTdp(name);
    }

    private ObjectNode constraintToOdrl(JsonNode constraint) {
        return mappings.applyTdpToDsp("constraintToOdrl", constraint, mappings.template("odrlConstraint"));
    }

    private ObjectNode odrlToConstraint(JsonNode constraint) {
        return mappings.applyDspToTdp("odrlToConstraint", constraint, mappings.template("constraint"));
    }

    private static boolean containsText(ArrayNode array, String value) {
        for (JsonNode item : array) {
            if (value.equals(item.asText())) {
                return true;
            }
        }
        return false;
    }

    /** DSP 里 permission / constraint 可能是单对象或数组，统一按列表迭代。 */
    private static Iterable<JsonNode> iterable(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            return node;
        }
        return List.of(node);
    }
}
