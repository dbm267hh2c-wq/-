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

@RestController
@RequestMapping("/dsp")
public class InboundDspController {

    private final InteropPipeline pipeline;

    public InboundDspController(InteropPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping("/catalog/request")
    public ResponseEntity<JsonNode> catalogRequest(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return inbound(DspMessageType.CATALOG_REQUEST, body, request, idsParticipant, destinationCountry);
    }

    @PostMapping("/catalog/datasets")
    public ResponseEntity<JsonNode> datasetRequest(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return inbound(DspMessageType.DATASET_REQUEST, body, request, idsParticipant, destinationCountry);
    }

    @PostMapping("/negotiations/request")
    public ResponseEntity<JsonNode> negotiationRequest(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return inbound(DspMessageType.CONTRACT_REQUEST, body, request, idsParticipant, destinationCountry);
    }

    @PostMapping("/transfers/request")
    public ResponseEntity<JsonNode> transferRequest(
            @RequestBody ObjectNode body,
            HttpServletRequest request,
            @RequestHeader(value = "X-IDS-Participant", required = false) String idsParticipant,
            @RequestHeader(value = "X-Destination-Country", required = false) String destinationCountry
    ) {
        return inbound(DspMessageType.TRANSFER_REQUEST, body, request, idsParticipant, destinationCountry);
    }

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
