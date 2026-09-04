package com.tdp.dsp.gateway.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;
import com.tdp.dsp.gateway.protocol.DspMessageType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 入境 HTTP 入口：境外 IDS / DSP 调用本网关，再转成国内可信数据空间操作。
 *
 * <p>路径对齐 Dataspace Protocol 的 Catalog / Negotiation / Transfer。
 * 报文 {@code @type} 由路径对应的 {@link DspMessageType} 指定，不从 JSON 猜测，避免类型伪装。
 *
 * <p>请求头：
 * <ul>
 *   <li>{@code X-IDS-Participant} — 对端 IDS DID，写入信封 metadata，供桥接层身份解析</li>
 *   <li>{@code X-Destination-Country} — 目的国，入境一般不强制，合规钩子主要看出境</li>
 * </ul>
 *
 * <p>合规拦截时桥接层返回 {@code CatalogError}，本控制器映射为 HTTP 403。
 */
@RestController
@RequestMapping("/dsp")
public class InboundDspController {

    private final InteropPipeline pipeline;

    public InboundDspController(InteropPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /** 目录检索：{@code CatalogRequestMessage} → 国内 {@code /catalogQuery}。 */
    @PostMapping("/catalog/request")
    public ResponseEntity<JsonNode> catalogRequest(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return inbound(DspMessageType.CATALOG_REQUEST, body, request, idsParticipant, destinationCountry);
    }

    /** 数据集详情：{@code DatasetRequestMessage} → 国内 {@code /productDetail}。 */
    @PostMapping("/catalog/datasets")
    public ResponseEntity<JsonNode> datasetRequest(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return inbound(DspMessageType.DATASET_REQUEST, body, request, idsParticipant, destinationCountry);
    }

    /** 合约协商发起：{@code ContractRequestMessage} → 国内 {@code /contractCreate}。 */
    @PostMapping("/negotiations/request")
    public ResponseEntity<JsonNode> negotiationRequest(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return inbound(DspMessageType.CONTRACT_REQUEST, body, request, idsParticipant, destinationCountry);
    }

    /** 传输发起：{@code TransferRequestMessage} → 国内 {@code /contractExecution}。 */
    @PostMapping("/transfers/request")
    public ResponseEntity<JsonNode> transferRequest(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return inbound(DspMessageType.TRANSFER_REQUEST, body, request, idsParticipant, destinationCountry);
    }

    /**
     * 组装 metadata 后进入四层流水线。返回体仍是 DSP JSON-LD，由协议层把国内结果转回。
     */
    private ResponseEntity<JsonNode> inbound(
            DspMessageType type,
            ObjectNode body,
            HttpServletRequest request,
            String idsParticipant,
            String destinationCountry
    ) {
        ObjectNode metadata = GatewayRequestSupport.metadata(request, idsParticipant, destinationCountry);
        ObjectNode response = pipeline.inboundDsp(type.typeName(), body, metadata);
        int status = "CatalogError".equals(Jsons.typeName(response)) ? 403 : 200;
        return ResponseEntity.status(status).body(response);
    }
}
