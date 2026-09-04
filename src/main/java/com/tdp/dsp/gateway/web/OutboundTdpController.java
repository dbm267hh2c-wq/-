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

@RestController
@RequestMapping("/tdp")
public class OutboundTdpController {

    private final InteropPipeline pipeline;

    public OutboundTdpController(InteropPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping("/catalogQuery")
    public ResponseEntity<JsonNode> catalogQuery(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CATALOG_QUERY, body, request, idsParticipant, destinationCountry);
    }

    @PostMapping("/productDetail")
    public ResponseEntity<JsonNode> productDetail(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.PRODUCT_DETAIL, body, request, idsParticipant, destinationCountry);
    }

    @PostMapping("/contractCreate")
    public ResponseEntity<JsonNode> contractCreate(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CONTRACT_CREATE, body, request, idsParticipant, destinationCountry);
    }

    @PostMapping("/contractNegotiate")
    public ResponseEntity<JsonNode> contractNegotiate(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CONTRACT_NEGOTIATE, body, request, idsParticipant, destinationCountry);
    }

    @PostMapping("/contractExecution")
    public ResponseEntity<JsonNode> contractExecution(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CONTRACT_EXECUTION, body, request, idsParticipant, destinationCountry);
    }

    @PostMapping("/contractTerminate")
    public ResponseEntity<JsonNode> contractTerminate(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return outbound(TdpOperation.CONTRACT_TERMINATE, body, request, idsParticipant, destinationCountry);
    }

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
