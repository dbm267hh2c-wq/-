package com.tdp.dsp.gateway.layer.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.model.dsp.DspMessages;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存模拟的 IDS 连接器，预置欧盟港口吞吐数据集，供出境目录 / 详情 / 协商联调。
 *
 * <p>按 {@code connectorUrl} 分目录；未知 URL 返回 CatalogError 404。
 * 协商与传输不落真实状态机，仅回稳定的 providerPid，便于断言。
 */
public class InMemoryIdsConnectorClient implements IdsConnectorClient {

    private final Map<String, ObjectNode> catalogs = new ConcurrentHashMap<>();

    public InMemoryIdsConnectorClient() {
        ObjectNode dataset = Jsons.object();
        dataset.put("@id", "urn:ids:dataset:eu-port-throughput");
        dataset.put("@type", "dcat:Dataset");
        dataset.put("dct:title", "EU Port Throughput");
        ArrayNode descriptions = Jsons.array();
        descriptions.add(Jsons.objectOf("@value", "Sample port logistics dataset", "@language", "en"));
        dataset.set("dct:description", descriptions);
        ArrayNode keywords = Jsons.array();
        keywords.add("logistics");
        keywords.add("port");
        dataset.set("dcat:keyword", keywords);
        ObjectNode offer = Jsons.object();
        offer.put("@id", "urn:uuid:" + UUID.nameUUIDFromBytes("eu-port-offer".getBytes()));
        offer.put("@type", "odrl:Offer");
        offer.put("odrl:assigner", "did:web:ids.example.eu:provider");
        offer.put("odrl:target", "urn:ids:dataset:eu-port-throughput");
        ArrayNode permissions = Jsons.array();
        ObjectNode permission = Jsons.object();
        permission.put("odrl:action", "odrl:use");
        ArrayNode constraints = Jsons.array();
        constraints.add(Jsons.objectOf(
                "odrl:leftOperand", "odrl:spatial",
                "odrl:operator", "odrl:eq",
                "odrl:rightOperand", "EU"
        ));
        permission.set("odrl:constraint", constraints);
        permissions.add(permission);
        offer.set("odrl:permission", permissions);
        ArrayNode policies = Jsons.array();
        policies.add(offer);
        dataset.set("odrl:hasPolicy", policies);
        ObjectNode distribution = Jsons.object();
        distribution.put("@type", "dcat:Distribution");
        distribution.put("dct:format", "HttpData-PULL");
        ObjectNode service = Jsons.object();
        service.put("@type", "dcat:DataService");
        service.put("dcat:endpointURL", "https://ids.example.eu/connector");
        ArrayNode services = Jsons.array();
        services.add(service);
        distribution.set("dcat:accessService", services);
        ArrayNode distributions = Jsons.array();
        distributions.add(distribution);
        dataset.set("dcat:distribution", distributions);

        ArrayNode datasets = Jsons.array();
        datasets.add(dataset);
        catalogs.put(
                "https://ids.example.eu/connector",
                DspMessages.catalog(
                        "urn:uuid:ids-eu-catalog",
                        "EU IDS Catalog",
                        "did:web:ids.example.eu:provider",
                        datasets,
                        "https://ids.example.eu/connector"
                )
        );
    }

    @Override
    public ObjectNode catalogRequest(String connectorUrl, ObjectNode dspMessage) {
        ObjectNode catalog = catalogs.get(connectorUrl);
        if (catalog == null) {
            return DspMessages.catalogError("404", "未知 IDS 连接器: " + connectorUrl);
        }
        return catalog.deepCopy();
    }

    @Override
    public ObjectNode datasetRequest(String connectorUrl, ObjectNode dspMessage) {
        ObjectNode catalog = catalogRequest(connectorUrl, dspMessage);
        String datasetId = Jsons.textOrEmpty(dspMessage, "dspace:dataset", "dataset");
        JsonNode datasets = catalog.get("dcat:dataset");
        if (datasets != null) {
            for (var dataset : datasets) {
                if (datasetId.equals(Jsons.textOrEmpty(dataset, "@id"))) {
                    return (ObjectNode) dataset;
                }
            }
        }
        return DspMessages.catalogError("404", "未找到数据集: " + datasetId);
    }

    @Override
    public ObjectNode negotiationRequest(String connectorUrl, ObjectNode dspMessage) {
        String consumerPid = Jsons.textOrEmpty(dspMessage, "dspace:consumerPid", "consumerPid");
        String datasetId = Jsons.textOrEmpty(dspMessage, "dspace:dataset", "dataset");
        String providerPid = "urn:uuid:" + UUID.nameUUIDFromBytes((connectorUrl + consumerPid).getBytes());
        return DspMessages.contractNegotiation(providerPid, consumerPid, "dspace:REQUESTED", datasetId);
    }

    @Override
    public ObjectNode transferRequest(String connectorUrl, ObjectNode dspMessage) {
        String consumerPid = Jsons.textOrEmpty(dspMessage, "dspace:consumerPid", "consumerPid");
        String agreementId = Jsons.textOrEmpty(dspMessage, "dspace:agreementId", "agreementId");
        String providerPid = "urn:uuid:" + UUID.nameUUIDFromBytes(("transfer-" + agreementId).getBytes());
        return DspMessages.transferProcess(providerPid, consumerPid, "dspace:STARTED", agreementId);
    }

    @Override
    public ObjectNode terminate(String connectorUrl, ObjectNode dspMessage) {
        String providerPid = Jsons.textOrEmpty(dspMessage, "dspace:providerPid", "providerPid");
        String consumerPid = Jsons.textOrEmpty(dspMessage, "dspace:consumerPid", "consumerPid");
        return DspMessages.contractNegotiation(providerPid, consumerPid, "dspace:TERMINATED", null);
    }

    public List<String> knownConnectors() {
        return List.copyOf(catalogs.keySet());
    }
}
