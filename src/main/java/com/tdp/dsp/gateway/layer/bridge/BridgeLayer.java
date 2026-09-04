package com.tdp.dsp.gateway.layer.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.layer.adapter.MessageAdapter;
import com.tdp.dsp.gateway.layer.protocol.ProtocolConverter;
import com.tdp.dsp.gateway.model.common.Direction;
import com.tdp.dsp.gateway.model.common.InteropEnvelope;
import com.tdp.dsp.gateway.model.common.Participant;
import com.tdp.dsp.gateway.model.dsp.DspMessages;
import com.tdp.dsp.gateway.protocol.DspMessageType;
import com.tdp.dsp.gateway.protocol.TdpOperation;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ③ 桥接层：桥接国内可信数据空间服务平台与国际数据空间（IDS）生态系统。
 */
public class BridgeLayer {

    private final Map<String, Participant> byTdpEntity = new ConcurrentHashMap<>();
    private final Map<String, Participant> byIdsParticipant = new ConcurrentHashMap<>();
    private final Map<String, Participant> byTdpConnector = new ConcurrentHashMap<>();
    private final TdpPlatformClient tdpPlatformClient;
    private final IdsConnectorClient idsConnectorClient;
    private final ProtocolConverter protocolConverter;
    private final MessageAdapter messageAdapter;

    public BridgeLayer(
            TdpPlatformClient tdpPlatformClient,
            IdsConnectorClient idsConnectorClient,
            ProtocolConverter protocolConverter,
            MessageAdapter messageAdapter
    ) {
        this.tdpPlatformClient = tdpPlatformClient;
        this.idsConnectorClient = idsConnectorClient;
        this.protocolConverter = protocolConverter;
        this.messageAdapter = messageAdapter;
        register(tdpPlatformClient.localParticipant());
    }

    public void register(Participant participant) {
        byTdpEntity.put(participant.tdpEntityId(), participant);
        byIdsParticipant.put(participant.idsParticipantId(), participant);
        byTdpConnector.put(participant.tdpConnectorId(), participant);
    }

    public Optional<Participant> findByTdpEntity(String entityId) {
        return Optional.ofNullable(byTdpEntity.get(entityId));
    }

    public Optional<Participant> findByIdsParticipant(String participantId) {
        return Optional.ofNullable(byIdsParticipant.get(participantId));
    }

    public Collection<Participant> participants() {
        return byTdpEntity.values();
    }

    public ObjectNode dispatch(InteropEnvelope envelope) {
        if (envelope.direction() == Direction.INBOUND) {
            return dispatchInbound(envelope);
        }
        return dispatchOutbound(envelope);
    }

    private ObjectNode dispatchInbound(InteropEnvelope envelope) {
        Participant local = tdpPlatformClient.localParticipant();
        ObjectNode context = contextOf(local, envelope);
        ObjectNode tdpRequest = protocolConverter.dspToTdp(envelope.payload(), context);
        String dspType = Jsons.typeName(envelope.payload());
        ObjectNode tdpResponse = invokeTdp(protocolConverter.tdpOperationForDsp(dspType), tdpRequest);
        return toDspResponse(dspType, tdpResponse, context, tdpRequest);
    }

    private ObjectNode dispatchOutbound(InteropEnvelope envelope) {
        Participant remote = resolveRemote(envelope);
        ObjectNode context = contextOf(tdpPlatformClient.localParticipant(), envelope);
        context.put("callbackAddress", tdpPlatformClient.localParticipant().idsConnectorUrl() + "/callback");
        ObjectNode dspRequest = protocolConverter.tdpToDsp(envelope.operation(), envelope.payload(), context);
        ObjectNode dspResponse = invokeIds(remote.idsConnectorUrl(), TdpOperation.fromCode(envelope.operation()), dspRequest);
        return toTdpResponse(envelope.operation(), dspResponse);
    }

    private ObjectNode invokeTdp(TdpOperation operation, ObjectNode request) {
        return switch (operation) {
            case CATALOG_QUERY -> tdpPlatformClient.catalogQuery(request);
            case PRODUCT_DETAIL -> tdpPlatformClient.productDetail(request);
            case CONTRACT_CREATE -> tdpPlatformClient.contractCreate(request);
            case CONTRACT_NEGOTIATE -> tdpPlatformClient.contractNegotiate(request);
            case CONTRACT_EXECUTION -> tdpPlatformClient.contractExecution(request);
            case CONTRACT_TERMINATE -> tdpPlatformClient.contractTerminate(request);
        };
    }

    private ObjectNode invokeIds(String connectorUrl, TdpOperation operation, ObjectNode dspRequest) {
        return switch (operation) {
            case CATALOG_QUERY -> idsConnectorClient.catalogRequest(connectorUrl, dspRequest);
            case PRODUCT_DETAIL -> idsConnectorClient.datasetRequest(connectorUrl, dspRequest);
            case CONTRACT_CREATE, CONTRACT_NEGOTIATE -> idsConnectorClient.negotiationRequest(connectorUrl, dspRequest);
            case CONTRACT_EXECUTION -> idsConnectorClient.transferRequest(connectorUrl, dspRequest);
            case CONTRACT_TERMINATE -> idsConnectorClient.terminate(connectorUrl, dspRequest);
        };
    }

    private ObjectNode toDspResponse(String dspType, ObjectNode tdpResponse, ObjectNode context, ObjectNode tdpRequest) {
        return switch (DspMessageType.fromTypeName(dspType)) {
            case CATALOG_REQUEST -> protocolConverter.tdpCatalogToDsp(
                    tdpResponse,
                    context.get("idsParticipantId").asText(),
                    context.get("idsConnectorUrl").asText()
            );
            case DATASET_REQUEST -> {
                JsonNode product = tdpResponse.get("product");
                yield product == null
                        ? DspMessages.catalogError("404", Jsons.textOrEmpty(tdpResponse, "message"))
                        : messageAdapter.productToDataset(product);
            }
            case CONTRACT_REQUEST, CONTRACT_AGREEMENT -> {
                String datasetId = "";
                if (tdpRequest.get("strategy") != null) {
                    datasetId = Jsons.textOrEmpty(tdpRequest.get("strategy").get("subjectInfo"), "dataProductId");
                }
                yield protocolConverter.tdpContractResultToDsp(
                        tdpResponse,
                        Jsons.textOrEmpty(context, "consumerPid"),
                        datasetId
                );
            }
            case TRANSFER_REQUEST, TRANSFER_START
                    -> protocolConverter.tdpExecutionToDsp(tdpResponse, Jsons.textOrEmpty(context, "consumerPid"));
            case CONTRACT_TERMINATION -> DspMessages.contractNegotiation(
                    Jsons.textOrEmpty(tdpResponse, "contractId"),
                    Jsons.textOrEmpty(context, "consumerPid"),
                    "dspace:TERMINATED",
                    null
            );
            default -> tdpResponse;
        };
    }

    private ObjectNode toTdpResponse(String tdpOperation, ObjectNode dspResponse) {
        return switch (TdpOperation.fromCode(tdpOperation)) {
            case CATALOG_QUERY -> protocolConverter.dspCatalogToTdp(dspResponse);
            case PRODUCT_DETAIL -> {
                ObjectNode response = Jsons.object();
                if ("CatalogError".equals(Jsons.typeName(dspResponse))) {
                    response.put("status", "1");
                    response.put("message", Jsons.textOrEmpty(dspResponse, "dspace:code"));
                    yield response;
                }
                response.put("status", "0");
                response.set("product", messageAdapter.datasetToProduct(dspResponse));
                yield response;
            }
            case CONTRACT_CREATE, CONTRACT_NEGOTIATE -> Jsons.objectOf(
                    "creationFlag", "0",
                    "negotiationStatus", "0",
                    "contractId", Jsons.textOrEmpty(dspResponse, "dspace:providerPid", "providerPid", "@id")
            );
            case CONTRACT_EXECUTION -> Jsons.objectOf(
                    "status", "0",
                    "transferId", Jsons.textOrEmpty(dspResponse, "dspace:providerPid", "providerPid", "@id"),
                    "contractId", Jsons.textOrEmpty(dspResponse, "dspace:agreementId", "agreementId")
            );
            case CONTRACT_TERMINATE -> Jsons.objectOf(
                    "status", "0",
                    "contractId", Jsons.textOrEmpty(dspResponse, "dspace:providerPid", "providerPid")
            );
        };
    }

    private Participant resolveRemote(InteropEnvelope envelope) {
        String idsId = Jsons.text(envelope.metadata(), "idsParticipantId");
        if (idsId != null) {
            return findByIdsParticipant(idsId).orElseThrow(
                    () -> new IllegalArgumentException("未注册的 IDS 参与方: " + idsId)
            );
        }
        String entityId = Jsons.text(envelope.payload(), "targetEntityId", "ownerEntityId");
        if (entityId != null) {
            return findByTdpEntity(entityId).orElseThrow(
                    () -> new IllegalArgumentException("未注册的境内主体: " + entityId)
            );
        }
        return byIdsParticipant.values().stream()
                .filter(item -> !"tdp".equals(item.dataspace()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未配置境外 IDS 参与方"));
    }

    private ObjectNode contextOf(Participant local, InteropEnvelope envelope) {
        ObjectNode context = Jsons.object();
        context.put("tdpEntityId", local.tdpEntityId());
        context.put("tdpConnectorId", local.tdpConnectorId());
        context.put("idsParticipantId", local.idsParticipantId());
        context.put("idsConnectorUrl", local.idsConnectorUrl());
        String consumerPid = Jsons.text(envelope.payload(), "dspace:consumerPid", "consumerPid");
        if (consumerPid != null) {
            context.put("consumerPid", consumerPid);
        }
        String callback = Jsons.text(envelope.payload(), "dspace:callbackAddress", "callbackAddress");
        if (callback != null) {
            context.put("callbackAddress", callback);
        }
        if (envelope.metadata() != null) {
            envelope.metadata().fields().forEachRemaining(entry -> context.set(entry.getKey(), entry.getValue()));
        }
        return context;
    }

    public ObjectNode directory() {
        ArrayNode items = Jsons.array();
        participants().forEach(participant -> items.add(Jsons.objectOf(
                "tdpEntityId", participant.tdpEntityId(),
                "tdpConnectorId", participant.tdpConnectorId(),
                "idsParticipantId", participant.idsParticipantId(),
                "idsConnectorUrl", participant.idsConnectorUrl(),
                "region", participant.region(),
                "dataspace", participant.dataspace()
        )));
        return Jsons.objectOf("participants", items);
    }
}
