package com.tdp.dsp.gateway.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;
import com.tdp.dsp.gateway.protocol.TdpOperation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 出境 HTTP 入口：国内平台按国标路径调用本网关，再转成 DSP 发往 IDS。
 *
 * <p>先按 {@link TdpOperation#schemaFile()} 做字段契约校验，不合格直接 HTTP 400，
 * 不进入合规关口，避免无效报文污染审计。无 Schema 文件的操作（协商/履行/终止）跳过该步。
 *
 * <p>{@code X-IDS-Participant} 指定境外对端；缺省时桥接层回落到已注册的非 {@code tdp} 参与方。
 * 国内报文 {@code status=1}（含合规拒绝）映射为 HTTP 403。
 */
@RestController
@RequestMapping("/tdp")
public class OutboundTdpController {

    private final InteropPipeline pipeline;

    public OutboundTdpController(InteropPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /** 目录查询 → {@code CatalogRequestMessage}。 */
    @PostMapping("/catalogQuery")
    public ResponseEntity<JsonNode> catalogQuery(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CATALOG_QUERY, body, request, idsParticipant, destinationCountry);
    }

    /** 产品详情 → {@code DatasetRequestMessage}。 */
    @PostMapping("/productDetail")
    public ResponseEntity<JsonNode> productDetail(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.PRODUCT_DETAIL, body, request, idsParticipant, destinationCountry);
    }

    /** 合约发起 → {@code ContractRequestMessage}。 */
    @PostMapping("/contractCreate")
    public ResponseEntity<JsonNode> contractCreate(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CONTRACT_CREATE, body, request, idsParticipant, destinationCountry);
    }

    /** 合约协商 → 同样走 {@code ContractRequestMessage}（DSP 侧协商态由协商过程消息表达）。 */
    @PostMapping("/contractNegotiate")
    public ResponseEntity<JsonNode> contractNegotiate(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CONTRACT_NEGOTIATE, body, request, idsParticipant, destinationCountry);
    }

    /** 合约履行 / 数据交付 → {@code TransferRequestMessage}。 */
    @PostMapping("/contractExecution")
    public ResponseEntity<JsonNode> contractExecution(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CONTRACT_EXECUTION, body, request, idsParticipant, destinationCountry);
    }

    /** 合约终止 → {@code ContractNegotiationTerminationMessage}。 */
    @PostMapping("/contractTerminate")
    public ResponseEntity<JsonNode> contractTerminate(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CONTRACT_TERMINATE, body, request, idsParticipant, destinationCountry);
    }

    /**
     * Schema 失败返回 {@code SCHEMA_INVALID}；通过后进入合规关口。
     * 合规拒绝的国内形态是 {@code status=1}，与平台业务失败共用该字段，HTTP 层统一按 403 处理。
     */
    private ResponseEntity<JsonNode> outbound(
            TdpOperation operation,
            ObjectNode body,
            HttpServletRequest request,
            String idsParticipant,
            String destinationCountry
    ) {
        List<String> schemaErrors = pipeline.schemaValidator().validate(operation.schemaFile(), body);
        if (!schemaErrors.isEmpty()) {
            ObjectNode error = Jsons.objectOf(
                    "status", "1",
                    "code", "SCHEMA_INVALID",
                    "message", "报文不符合字段契约",
                    "errors", schemaErrors
            );
            return ResponseEntity.badRequest().body(error);
        }
        ObjectNode metadata = GatewayRequestSupport.metadata(request, idsParticipant, destinationCountry);
        ObjectNode response = pipeline.outboundTdp(operation.code(), body, metadata);
        int status = "1".equals(Jsons.text(response, "status")) ? 403 : 200;
        return ResponseEntity.status(status).body(response);
    }
}
