package com.tdp.dsp.gateway.layer.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.constant.ProtocolConstants;
import com.tdp.dsp.gateway.json.Jsons;

import java.util.List;
import java.util.UUID;

/**
 * ② 消息适配层：国内数据接口与 DSP 的消息格式适配、字段映射、语义转换。
 */
public class MessageAdapter {

    public ObjectNode productToDataset(JsonNode product) {
        ObjectNode dataset = Jsons.object();
        String productId = Jsons.textOrEmpty(product, "dataProductId");
        dataset.put("@id", productId);
        dataset.put("@type", "dcat:Dataset");
        dataset.put("dct:title", Jsons.textOrEmpty(product, "dataProductName"));
        ArrayNode descriptions = Jsons.array();
        descriptions.add(Jsons.objectOf(
                "@value", Jsons.textOrEmpty(product, "dataProductAbstract"),
                "@language", "zh"
        ));
        dataset.set("dct:description", descriptions);
        ArrayNode keywords = Jsons.array();
        Jsons.stringList(Jsons.get(product, "keywords")).forEach(keywords::add);
        dataset.set("dcat:keyword", keywords);

        JsonNode strategy = Jsons.get(product, "strategy");
        ArrayNode policies = Jsons.array();
        policies.add(strategyToOffer(strategy, productId, Jsons.textOrEmpty(product, "ownerEntityId")));
        dataset.set("odrl:hasPolicy", policies);

        ObjectNode distribution = Jsons.object();
        distribution.put("@type", "dcat:Distribution");
        distribution.put("dct:format", Jsons.textOrEmpty(product, "format"));
        ObjectNode accessService = Jsons.object();
        accessService.put("@type", "dcat:DataService");
        accessService.put("dspace:dataServiceType", "dspace:connector");
        accessService.put("dcat:endpointURL", Jsons.textOrEmpty(product, "endpointUrl"));
        ArrayNode services = Jsons.array();
        services.add(accessService);
        distribution.set("dcat:accessService", services);
        ArrayNode distributions = Jsons.array();
        distributions.add(distribution);
        dataset.set("dcat:distribution", distributions);
        return dataset;
    }

    public ObjectNode datasetToProduct(JsonNode dataset) {
        ObjectNode product = Jsons.object();
        product.put("dataProductId", Jsons.textOrEmpty(dataset, "@id"));
        product.put("dataProductName", Jsons.textOrEmpty(dataset, "dct:title", "title"));
        product.put("dataProductAbstract", firstDescription(dataset));
        product.set("keywords", copyStrings(Jsons.get(dataset, "dcat:keyword", "keyword")));
        JsonNode policy = first(Jsons.get(dataset, "odrl:hasPolicy", "hasPolicy"));
        if (policy != null) {
            product.put("ownerEntityId", Jsons.textOrEmpty(policy, "odrl:assigner", "assigner"));
            product.set("strategy", offerToStrategy(policy));
        }
        JsonNode distribution = first(Jsons.get(dataset, "dcat:distribution", "distribution"));
        if (distribution != null) {
            product.put("format", Jsons.textOrEmpty(distribution, "dct:format", "format"));
            JsonNode service = first(Jsons.get(distribution, "dcat:accessService", "accessService"));
            if (service != null) {
                product.put("endpointUrl", Jsons.textOrEmpty(service, "dcat:endpointURL", "endpointURL"));
            }
        }
        return product;
    }

    public ObjectNode strategyToOffer(JsonNode strategy, String targetId, String assigner) {
        ObjectNode offer = Jsons.object();
        offer.put("@id", "urn:uuid:" + UUID.nameUUIDFromBytes((targetId + "-offer").getBytes()));
        offer.put("@type", "odrl:Offer");
        offer.put("odrl:assigner", assigner == null ? "" : assigner);
        offer.put("odrl:target", targetId);
        ArrayNode permissions = Jsons.array();
        List<String> actions = Jsons.stringList(Jsons.get(strategy, "actions"));
        if (actions.isEmpty()) {
            actions = List.of("授权使用");
        }
        for (String action : actions) {
            ObjectNode permission = Jsons.object();
            permission.put("odrl:action", mapActionToOdrl(action));
            ArrayNode constraints = Jsons.array();
            JsonNode rawConstraints = Jsons.get(strategy, "constraints");
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

    public ObjectNode offerToStrategy(JsonNode offer) {
        ObjectNode strategy = Jsons.object();
        ObjectNode subject = Jsons.object();
        subject.put("dataProductId", Jsons.textOrEmpty(offer, "odrl:target", "target"));
        strategy.set("subjectInfo", subject);
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
        ObjectNode nodes = Jsons.object();
        strategy.set("executionNodeInfo", nodes);
        return strategy;
    }

    public ObjectNode catalogQueryToDspFilter(JsonNode catalogQuery) {
        ObjectNode request = Jsons.object();
        ArrayNode filters = Jsons.array();
        String keyword = Jsons.text(catalogQuery, "keyword");
        if (keyword != null && !keyword.isBlank()) {
            filters.add(keyword);
        }
        JsonNode existing = Jsons.get(catalogQuery, "filter");
        if (existing != null) {
            Jsons.stringList(existing).forEach(filters::add);
        }
        if (!filters.isEmpty()) {
            request.set("dspace:filter", filters);
        }
        request.put("issuerId", Jsons.textOrEmpty(catalogQuery, "issuerId"));
        request.put("issuerEntityId", Jsons.textOrEmpty(catalogQuery, "issuerEntityId"));
        return request;
    }

    public ObjectNode dspFilterToCatalogQuery(JsonNode catalogRequest, String issuerId, String issuerEntityId) {
        ObjectNode query = Jsons.object();
        query.put("issuerId", issuerId);
        query.put("issuerEntityId", issuerEntityId);
        List<String> filters = Jsons.stringList(Jsons.get(catalogRequest, "dspace:filter", "filter"));
        if (!filters.isEmpty()) {
            query.put("keyword", filters.get(0));
            ArrayNode filterNode = Jsons.array();
            filters.forEach(filterNode::add);
            query.set("filter", filterNode);
        }
        query.put("pageNum", 1);
        query.put("pageSize", 20);
        return query;
    }

    public ObjectNode contractCreateToContractRequest(JsonNode create, String consumerPid) {
        JsonNode strategy = Jsons.get(create, "strategy");
        String productId = Jsons.textOrEmpty(strategy, "subjectInfo");
        if (strategy != null) {
            productId = Jsons.textOrEmpty(Jsons.get(strategy, "subjectInfo"), "dataProductId");
        }
        ObjectNode offer = strategyToOffer(strategy, productId, Jsons.textOrEmpty(create, "issuerEntityId"));
        return Jsons.objectOf(
                "consumerPid", consumerPid,
                "callbackAddress", Jsons.textOrEmpty(create, "callbackAddress"),
                "datasetId", productId,
                "offer", offer,
                "contractName", Jsons.textOrEmpty(create, "contractName")
        );
    }

    public String mapActionToOdrl(String action) {
        if (action == null || action.isBlank()) {
            return "odrl:use";
        }
        return ProtocolConstants.TDP_ACTION_TO_ODRL.getOrDefault(action, "odrl:use");
    }

    public String mapActionToTdp(String action) {
        if (action == null || action.isBlank()) {
            return "授权使用";
        }
        String shortName = action.contains(":") ? action : "odrl:" + action;
        return ProtocolConstants.ODRL_ACTION_TO_TDP.getOrDefault(shortName, "授权使用");
    }

    public String mapOperatorToOdrl(String operator) {
        return ProtocolConstants.TDP_OPERATOR_TO_ODRL.getOrDefault(operator, "odrl:eq");
    }

    public String mapOperatorToTdp(String operator) {
        if (operator == null) {
            return "01";
        }
        String key = operator.contains(":") ? operator : "odrl:" + operator;
        return ProtocolConstants.ODRL_OPERATOR_TO_TDP.getOrDefault(key, "01");
    }

    public String mapConstraintNameToOdrl(String name) {
        return ProtocolConstants.TDP_CONSTRAINT_TO_ODRL.getOrDefault(name, name);
    }

    public String mapConstraintNameToTdp(String name) {
        if (name == null) {
            return "";
        }
        String key = name.contains(":") ? name : "odrl:" + name;
        return ProtocolConstants.ODRL_CONSTRAINT_TO_TDP.getOrDefault(key, name);
    }

    private ObjectNode constraintToOdrl(JsonNode constraint) {
        ObjectNode node = Jsons.object();
        node.put("odrl:leftOperand", mapConstraintNameToOdrl(Jsons.textOrEmpty(constraint, "constraintName")));
        node.put("odrl:operator", mapOperatorToOdrl(Jsons.textOrEmpty(constraint, "constraintOperator")));
        node.put("odrl:rightOperand", Jsons.textOrEmpty(constraint, "constraintValue"));
        return node;
    }

    private ObjectNode odrlToConstraint(JsonNode constraint) {
        ObjectNode node = Jsons.object();
        node.put("constraintName", mapConstraintNameToTdp(Jsons.textOrEmpty(constraint, "odrl:leftOperand", "leftOperand")));
        node.put("constraintOperator", mapOperatorToTdp(Jsons.textOrEmpty(constraint, "odrl:operator", "operator")));
        node.put("constraintValue", Jsons.textOrEmpty(constraint, "odrl:rightOperand", "rightOperand"));
        return node;
    }

    private static String firstDescription(JsonNode dataset) {
        JsonNode description = Jsons.get(dataset, "dct:description", "description");
        if (description == null) {
            return "";
        }
        JsonNode first = first(description);
        if (first == null) {
            return "";
        }
        if (first.isTextual()) {
            return first.asText();
        }
        return Jsons.textOrEmpty(first, "@value");
    }

    private static ArrayNode copyStrings(JsonNode node) {
        ArrayNode array = Jsons.array();
        Jsons.stringList(node).forEach(array::add);
        return array;
    }

    private static JsonNode first(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return node.isEmpty() ? null : node.get(0);
        }
        return node;
    }

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
