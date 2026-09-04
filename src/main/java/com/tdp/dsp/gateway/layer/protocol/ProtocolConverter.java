package com.tdp.dsp.gateway.layer.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.constant.ProtocolConstants;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.layer.adapter.MessageAdapter;
import com.tdp.dsp.gateway.model.dsp.DspMessages;
import com.tdp.dsp.gateway.protocol.DspMessageType;
import com.tdp.dsp.gateway.protocol.TdpOperation;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ① 协议转换层：国内操作 ↔ DSP 消息类型，并组装完整报文骨架。
 *
 * <p>本层决定「这是哪一种对话」（目录 / 协商 / 传输），字段语义交给 {@link MessageAdapter}。
 * 一对多关系写在 switch 里：例如国内「合约发起」与「合约协商」都对应 DSP
 * {@code ContractRequestMessage}；入境 {@code TransferRequest} 与 {@code TransferStart}
 * 都落到国内履行。
 *
 * <p>入境创建合约时补国内必填的合约名、时效、平台签署模式，因为 DSP 请求没有对等字段。
 */
public class ProtocolConverter {

    private final MessageAdapter adapter;

    public ProtocolConverter(MessageAdapter adapter) {
        this.adapter = adapter;
    }

    public String tdpOperationToDspType(String tdpOperation) {
        return toDspType(TdpOperation.fromCode(tdpOperation)).typeName();
    }

    /** 国内操作 → DSP 请求/过程类型（出境调用 IDS 时用）。 */
    public DspMessageType toDspType(TdpOperation operation) {
        return switch (operation) {
            case CATALOG_QUERY -> DspMessageType.CATALOG_REQUEST;
            case PRODUCT_DETAIL -> DspMessageType.DATASET_REQUEST;
            case CONTRACT_CREATE, CONTRACT_NEGOTIATE -> DspMessageType.CONTRACT_REQUEST;
            case CONTRACT_EXECUTION -> DspMessageType.TRANSFER_REQUEST;
            case CONTRACT_TERMINATE -> DspMessageType.CONTRACT_TERMINATION;
        };
    }

    public String dspTypeToTdpOperation(String dspType) {
        return toTdpOperation(DspMessageType.fromTypeName(dspType)).code();
    }

    /**
     * DSP 类型 → 国内操作（入境调用 TDP 时用）。
     * Catalog / Dataset / ContractNegotiation 等过程对象不是请求，转换时会抛错。
     */
    public TdpOperation toTdpOperation(DspMessageType type) {
        return switch (type) {
            case CATALOG_REQUEST -> TdpOperation.CATALOG_QUERY;
            case DATASET_REQUEST -> TdpOperation.PRODUCT_DETAIL;
            case CONTRACT_REQUEST -> TdpOperation.CONTRACT_CREATE;
            case CONTRACT_AGREEMENT -> TdpOperation.CONTRACT_NEGOTIATE;
            case TRANSFER_REQUEST, TRANSFER_START -> TdpOperation.CONTRACT_EXECUTION;
            case CONTRACT_TERMINATION -> TdpOperation.CONTRACT_TERMINATE;
            default -> throw new IllegalArgumentException("不支持的 DSP 消息类型: " + type);
        };
    }

    public String dspPathForTdpOperation(String tdpOperation) {
        return toDspType(TdpOperation.fromCode(tdpOperation)).dspPath();
    }

    public String tdpPathForDspType(String dspType) {
        return toTdpOperation(DspMessageType.fromTypeName(dspType)).domesticPath();
    }

    public TdpOperation tdpOperationForDsp(String dspType) {
        return toTdpOperation(DspMessageType.fromTypeName(dspType));
    }

    public ObjectNode tdpToDsp(String tdpOperation, JsonNode tdpPayload, ObjectNode context) {
        return tdpToDsp(TdpOperation.fromCode(tdpOperation), tdpPayload, context);
    }

    /**
     * 国内请求体 + 桥接上下文 → DSP 请求。
     * {@code context} 提供 callback、consumerPid 等 DSP 控制面字段。
     */
    public ObjectNode tdpToDsp(TdpOperation operation, JsonNode tdpPayload, ObjectNode context) {
        return switch (operation) {
            case CATALOG_QUERY -> toCatalogRequest(tdpPayload);
            case PRODUCT_DETAIL -> DspMessages.datasetRequest(Jsons.textOrEmpty(tdpPayload, "dataProductId"));
            case CONTRACT_CREATE, CONTRACT_NEGOTIATE -> toContractRequest(tdpPayload, context);
            case CONTRACT_EXECUTION -> toTransferRequest(tdpPayload, context);
            case CONTRACT_TERMINATE -> DspMessages.contractTermination(
                    Jsons.textOrEmpty(tdpPayload, "contractId"),
                    Jsons.textOrEmpty(context, "consumerPid"),
                    "terminated",
                    Jsons.textOrEmpty(tdpPayload, "reason")
            );
        };
    }

    /**
     * DSP 请求 + 本侧身份上下文 → 国内请求体。
     */
    public ObjectNode dspToTdp(JsonNode dspMessage, ObjectNode context) {
        DspMessageType type = DspMessageType.fromTypeName(Jsons.typeName(dspMessage));
        return switch (type) {
            case CATALOG_REQUEST -> adapter.dspFilterToCatalogQuery(
                    dspMessage,
                    Jsons.textOrEmpty(context, "tdpConnectorId"),
                    Jsons.textOrEmpty(context, "tdpEntityId")
            );
            case DATASET_REQUEST -> Jsons.objectOf(
                    "dataProductId", Jsons.textOrEmpty(dspMessage, "dspace:dataset", "dataset"),
                    "issuerId", Jsons.textOrEmpty(context, "tdpConnectorId"),
                    "issuerEntityId", Jsons.textOrEmpty(context, "tdpEntityId")
            );
            case CONTRACT_REQUEST -> toContractCreate(dspMessage, context);
            case CONTRACT_AGREEMENT -> toContractNegotiate(dspMessage, context);
            case TRANSFER_REQUEST, TRANSFER_START -> toContractExecution(dspMessage, context);
            case CONTRACT_TERMINATION -> Jsons.objectOf(
                    "contractId", Jsons.textOrEmpty(dspMessage, "dspace:providerPid", "providerPid"),
                    "reason", firstReason(dspMessage),
                    "issuerId", Jsons.textOrEmpty(context, "tdpConnectorId")
            );
            default -> throw new IllegalArgumentException("不支持的 DSP 消息类型: " + type);
        };
    }

    /** 国内目录查询结果 → DCAT Catalog，产品列表逐条适配为 Dataset。 */
    public ObjectNode tdpCatalogToDsp(JsonNode tdpCatalog, String participantId, String connectorUrl) {
        ArrayNode datasets = Jsons.array();
        JsonNode products = Jsons.get(tdpCatalog, "products");
        if (products != null) {
            products.forEach(product -> datasets.add(adapter.productToDataset(product)));
        }
        return DspMessages.catalog(
                "urn:uuid:" + UUID.nameUUIDFromBytes(participantId.getBytes()),
                "Trusted Data Space Catalog",
                participantId,
                datasets,
                connectorUrl
        );
    }

    public ObjectNode dspCatalogToTdp(JsonNode dspCatalog) {
        ObjectNode response = Jsons.object();
        response.put("status", "0");
        ArrayNode products = Jsons.array();
        JsonNode datasets = Jsons.get(dspCatalog, "dcat:dataset", "dataset");
        if (datasets != null) {
            for (JsonNode dataset : datasets) {
                products.add(adapter.datasetToProduct(dataset));
            }
        }
        response.set("products", products);
        response.put("total", products.size());
        return response;
    }

    /**
     * 国内 {@code creationFlag}/{@code negotiationStatus}：{@code 0} 或空视为协商已受理。
     */
    public ObjectNode tdpContractResultToDsp(JsonNode tdpResult, String consumerPid, String datasetId) {
        String contractId = Jsons.textOrEmpty(tdpResult, "contractId");
        String flag = Jsons.textOrEmpty(tdpResult, "creationFlag", "negotiationStatus");
        String state = "0".equals(flag) || flag.isBlank() ? "dspace:REQUESTED" : "dspace:TERMINATED";
        return DspMessages.contractNegotiation(contractId, consumerPid, state, datasetId);
    }

    public ObjectNode tdpExecutionToDsp(JsonNode tdpResult, String consumerPid) {
        String transferId = Jsons.textOrEmpty(tdpResult, "transferId", "contractId");
        return DspMessages.transferProcess(transferId, consumerPid, "dspace:STARTED",
                Jsons.textOrEmpty(tdpResult, "contractId"));
    }

    private ObjectNode toCatalogRequest(JsonNode tdpPayload) {
        List<String> filters = Jsons.stringList(Jsons.get(tdpPayload, "filter"));
        String keyword = Jsons.text(tdpPayload, "keyword");
        if (keyword != null && !keyword.isBlank() && !filters.contains(keyword)) {
            filters.add(0, keyword);
        }
        return DspMessages.catalogRequest(filters);
    }

    private ObjectNode toContractRequest(JsonNode tdpPayload, ObjectNode context) {
        JsonNode strategy = Jsons.get(tdpPayload, "strategy");
        String productId = "";
        if (strategy != null) {
            productId = Jsons.textOrEmpty(Jsons.get(strategy, "subjectInfo"), "dataProductId");
        }
        ObjectNode offer = adapter.strategyToOffer(
                strategy,
                productId,
                Jsons.textOrEmpty(tdpPayload, "issuerEntityId")
        );
        String consumerPid = Jsons.textOrEmpty(context, "consumerPid");
        if (consumerPid.isBlank()) {
            // DSP 要求消费者进程 ID；国内报文没有对等字段时由网关签发
            consumerPid = "urn:uuid:" + UUID.randomUUID();
        }
        String callback = Jsons.textOrEmpty(tdpPayload, "callbackAddress");
        if (callback.isBlank()) {
            callback = Jsons.textOrEmpty(context, "callbackAddress");
        }
        return DspMessages.contractRequest(consumerPid, offer, callback, productId);
    }

    private ObjectNode toTransferRequest(JsonNode tdpPayload, ObjectNode context) {
        String consumerPid = Jsons.textOrEmpty(context, "consumerPid");
        if (consumerPid.isBlank()) {
            consumerPid = "urn:uuid:" + UUID.randomUUID();
        }
        String callback = Jsons.textOrEmpty(context, "callbackAddress");
        return DspMessages.transferRequest(
                consumerPid,
                Jsons.textOrEmpty(tdpPayload, "contractId"),
                Jsons.textOrEmpty(tdpPayload, "transferType"),
                callback,
                Jsons.get(tdpPayload, "dataAddress")
        );
    }

    /**
     * 入境合约请求：补国内 Schema 所需的名称、时效、平台签署。有效期默认 30 天。
     */
    private ObjectNode toContractCreate(JsonNode dspMessage, ObjectNode context) {
        JsonNode offer = Jsons.get(dspMessage, "odrl:offer", "offer");
        ObjectNode strategy = adapter.offerToStrategy(offer == null ? Jsons.object() : offer);
        ObjectNode subject = Jsons.requireObject(strategy.get("subjectInfo"));
        if (subject.path("dataProductId").asText("").isBlank()) {
            subject.put("dataProductId", Jsons.textOrEmpty(dspMessage, "dspace:dataset", "dataset"));
        }
        Instant now = Instant.now();
        ObjectNode create = Jsons.object();
        create.put("contractName", "跨境数据使用合约-" + subject.path("dataProductId").asText());
        create.put("contractAbstract", "由 DSP ContractRequestMessage 转换生成");
        create.put("issueTime", now.toString());
        create.put("activationTime", now.toString());
        create.put("endTime", now.plusSeconds(30L * 24 * 3600).toString());
        create.put("signMode", ProtocolConstants.SIGN_MODE_PLATFORM);
        create.put("issuerId", Jsons.textOrEmpty(context, "tdpConnectorId"));
        create.put("issuerEntityId", Jsons.textOrEmpty(context, "tdpEntityId"));
        create.put("signature", "dsp-bridge");
        create.put("callbackAddress", Jsons.textOrEmpty(dspMessage, "dspace:callbackAddress", "callbackAddress"));
        create.set("strategy", strategy);
        return create;
    }

    private ObjectNode toContractNegotiate(JsonNode dspMessage, ObjectNode context) {
        JsonNode agreement = Jsons.get(dspMessage, "dspace:agreement", "agreement");
        ObjectNode strategy = adapter.offerToStrategy(agreement == null ? Jsons.object() : agreement);
        ObjectNode negotiate = Jsons.object();
        negotiate.put("contractName", "跨境数据使用合约");
        negotiate.put("contractId", Jsons.textOrEmpty(dspMessage, "dspace:providerPid", "providerPid"));
        negotiate.set("strategy", strategy);
        ArrayNode signatures = Jsons.array();
        signatures.add(Jsons.objectOf(
                "entityId", Jsons.textOrEmpty(context, "tdpEntityId"),
                "signature", "dsp-agreement",
                "signatoryTime", Instant.now().toString()
        ));
        negotiate.set("signatureList", signatures);
        return negotiate;
    }

    private ObjectNode toContractExecution(JsonNode dspMessage, ObjectNode context) {
        ObjectNode execution = Jsons.object();
        execution.put("contractId", Jsons.textOrEmpty(
                dspMessage, "dspace:agreementId", "agreementId", "dspace:providerPid", "providerPid"
        ));
        execution.put("dataProductId", Jsons.textOrEmpty(context, "dataProductId"));
        execution.put("providerNodeId", Jsons.textOrEmpty(context, "tdpConnectorId"));
        execution.put("consumerNodeId", Jsons.textOrEmpty(dspMessage, "dspace:consumerPid", "consumerPid"));
        String format = Jsons.textOrEmpty(dspMessage, "dct:format", "format");
        execution.put("transferType", format.isBlank() ? "HttpData-PULL" : format);
        JsonNode dataAddress = Jsons.get(dspMessage, "dspace:dataAddress", "dataAddress");
        execution.set("dataAddress", dataAddress == null ? Jsons.object() : dataAddress);
        return execution;
    }

    /** DSP {@code dspace:reason} 可能是语言标签对象数组，取第一条文本或 {@code @value}。 */
    private static String firstReason(JsonNode message) {
        JsonNode reasons = Jsons.get(message, "dspace:reason", "reason");
        if (reasons == null || reasons.isEmpty()) {
            return "";
        }
        JsonNode first = reasons.isArray() ? reasons.get(0) : reasons;
        if (first.isTextual()) {
            return first.asText();
        }
        return Jsons.textOrEmpty(first, "@value");
    }

    /**
     * 粗判是否为 DSP JSON-LD，供调试或后续自动路由；正式入境仍以路径绑定的枚举为准。
     */
    public boolean isDspMessage(JsonNode payload) {
        if (payload == null || !payload.has("@type")) {
            return false;
        }
        String type = Jsons.typeName(payload).toLowerCase(Locale.ROOT);
        return type.contains("catalog")
                || type.contains("dataset")
                || type.contains("contract")
                || type.contains("transfer");
    }
}
