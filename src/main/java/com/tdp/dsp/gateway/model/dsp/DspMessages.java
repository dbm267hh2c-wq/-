package com.tdp.dsp.gateway.model.dsp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.constant.ProtocolConstants;
import com.tdp.dsp.gateway.json.Jsons;

import static com.tdp.dsp.gateway.json.Jsons.object;
import static com.tdp.dsp.gateway.json.Jsons.put;

/**
 * DSP JSON-LD 消息工厂（紧凑形态：带 {@code dspace:}/{@code odrl:}/{@code dcat:} 前缀）。
 *
 * <p>只负责骨架与必填控制面字段；业务叶子由适配层填入。
 * 所有报文共享 {@link ProtocolConstants#DSP_CONTEXT}。
 */
public final class DspMessages {

    private DspMessages() {
    }

    /**
     * 无前缀的 type 会自动补 {@code dspace:}；已含冒号的（如 {@code dcat:Catalog}）保持原样。
     */
    public static ObjectNode base(String type) {
        ObjectNode payload = object();
        payload.put("@context", ProtocolConstants.DSP_CONTEXT);
        payload.put("@type", type.contains(":") ? type : "dspace:" + type);
        return payload;
    }

    public static ObjectNode catalogRequest(Iterable<String> filters) {
        ObjectNode payload = base("dspace:CatalogRequestMessage");
        if (filters != null) {
            ArrayNode filterNode = Jsons.array();
            filters.forEach(filterNode::add);
            if (!filterNode.isEmpty()) {
                payload.set("dspace:filter", filterNode);
            }
        }
        return payload;
    }

    public static ObjectNode datasetRequest(String datasetId) {
        ObjectNode payload = base("dspace:DatasetRequestMessage");
        payload.put("dspace:dataset", datasetId);
        return payload;
    }

    /**
     * 目录/合规错误。{@code dspace:reason} 使用语言标签对象数组，符合 DSP 多语言习惯。
     */
    public static ObjectNode catalogError(String code, String reason) {
        ObjectNode payload = base("dspace:CatalogError");
        payload.put("dspace:code", code);
        ArrayNode reasons = Jsons.array();
        if (reason != null && !reason.isBlank()) {
            reasons.add(Jsons.objectOf("@value", reason));
        }
        payload.set("dspace:reason", reasons);
        return payload;
    }

    public static ObjectNode contractRequest(
            String consumerPid,
            ObjectNode offer,
            String callbackAddress,
            String datasetId
    ) {
        ObjectNode payload = base("dspace:ContractRequestMessage");
        payload.put("dspace:consumerPid", consumerPid);
        payload.put("dspace:callbackAddress", callbackAddress);
        payload.set("odrl:offer", offer);
        if (datasetId != null && !datasetId.isBlank()) {
            payload.put("dspace:dataset", datasetId);
        }
        return payload;
    }

    public static ObjectNode contractNegotiation(
            String providerPid,
            String consumerPid,
            String state,
            String datasetId
    ) {
        ObjectNode payload = base("dspace:ContractNegotiation");
        payload.put("@id", providerPid);
        payload.put("dspace:providerPid", providerPid);
        payload.put("dspace:consumerPid", consumerPid);
        payload.put("dspace:state", state);
        if (datasetId != null && !datasetId.isBlank()) {
            payload.put("dspace:dataset", datasetId);
        }
        return payload;
    }

    public static ObjectNode contractAgreement(
            String providerPid,
            String consumerPid,
            ObjectNode agreement,
            String callbackAddress
    ) {
        ObjectNode payload = base("dspace:ContractAgreementMessage");
        payload.put("dspace:providerPid", providerPid);
        payload.put("dspace:consumerPid", consumerPid);
        payload.put("dspace:callbackAddress", callbackAddress);
        payload.set("dspace:agreement", agreement);
        return payload;
    }

    public static ObjectNode contractTermination(
            String providerPid,
            String consumerPid,
            String code,
            String reason
    ) {
        ObjectNode payload = base("dspace:ContractNegotiationTerminationMessage");
        payload.put("dspace:providerPid", providerPid);
        payload.put("dspace:consumerPid", consumerPid);
        payload.put("dspace:code", code);
        ArrayNode reasons = Jsons.array();
        if (reason != null && !reason.isBlank()) {
            reasons.add(Jsons.objectOf("@value", reason));
        }
        payload.set("dspace:reason", reasons);
        return payload;
    }

    public static ObjectNode transferRequest(
            String consumerPid,
            String agreementId,
            String format,
            String callbackAddress,
            JsonNode dataAddress
    ) {
        ObjectNode payload = base("dspace:TransferRequestMessage");
        payload.put("dspace:consumerPid", consumerPid);
        payload.put("dspace:agreementId", agreementId);
        payload.put("dct:format", format);
        payload.put("dspace:callbackAddress", callbackAddress);
        if (dataAddress != null) {
            payload.set("dspace:dataAddress", dataAddress);
        }
        return payload;
    }

    public static ObjectNode transferProcess(
            String providerPid,
            String consumerPid,
            String state,
            String agreementId
    ) {
        ObjectNode payload = base("dspace:TransferProcess");
        payload.put("@id", providerPid);
        payload.put("dspace:providerPid", providerPid);
        payload.put("dspace:consumerPid", consumerPid);
        payload.put("dspace:state", state);
        if (agreementId != null && !agreementId.isBlank()) {
            payload.put("dspace:agreementId", agreementId);
        }
        return payload;
    }

    public static ObjectNode transferStart(String providerPid, String consumerPid, JsonNode dataAddress) {
        ObjectNode payload = base("dspace:TransferStartMessage");
        payload.put("dspace:providerPid", providerPid);
        payload.put("dspace:consumerPid", consumerPid);
        if (dataAddress != null) {
            payload.set("dspace:dataAddress", dataAddress);
        }
        return payload;
    }

    public static ObjectNode catalog(
            String catalogId,
            String title,
            String participantId,
            ArrayNode datasets,
            String serviceUrl
    ) {
        ObjectNode payload = base("dcat:Catalog");
        payload.put("@id", catalogId);
        payload.put("dct:title", title);
        payload.put("dspace:participantId", participantId);
        ObjectNode service = object();
        put(service, "@type", "dcat:DataService");
        put(service, "dspace:dataServiceType", "dspace:connector");
        put(service, "dcat:endpointURL", serviceUrl);
        ArrayNode services = Jsons.array();
        services.add(service);
        payload.set("dcat:service", services);
        payload.set("dcat:dataset", datasets == null ? Jsons.array() : datasets);
        return payload;
    }
}
