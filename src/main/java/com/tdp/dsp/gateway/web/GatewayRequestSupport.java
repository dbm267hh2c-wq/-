package com.tdp.dsp.gateway.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import jakarta.servlet.http.HttpServletRequest;

final class GatewayRequestSupport {

    private GatewayRequestSupport() {
    }

    static ObjectNode metadata(HttpServletRequest request, String idsParticipant, String destinationCountry) {
        ObjectNode metadata = Jsons.object();
        if (idsParticipant != null && !idsParticipant.isBlank()) {
            metadata.put("idsParticipantId", idsParticipant);
        }
        if (destinationCountry != null && !destinationCountry.isBlank()) {
            metadata.put("destinationCountry", destinationCountry);
        }
        metadata.put("peer", request.getRemoteAddr());
        return metadata;
    }
}
