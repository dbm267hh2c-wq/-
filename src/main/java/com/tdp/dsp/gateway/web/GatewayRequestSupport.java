package com.tdp.dsp.gateway.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 出入境 Controller 共用的信封 metadata 组装。
 *
 * <p>只抽取跨层真正会读的键：{@code idsParticipantId}、{@code destinationCountry}、{@code peer}。
 * 业务字段留在 body，避免把 HTTP 头和报文混在同一层。
 */
final class GatewayRequestSupport {

    private GatewayRequestSupport() {
    }

    /**
     * @param request             用于记录对端 IP，写入审计 peer
     * @param idsParticipant      请求头 {@code X-IDS-Participant}，可空
     * @param destinationCountry  请求头 {@code X-Destination-Country}，可空
     */
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
