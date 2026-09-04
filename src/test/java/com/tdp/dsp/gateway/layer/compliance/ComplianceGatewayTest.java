package com.tdp.dsp.gateway.layer.compliance;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;
import com.tdp.dsp.gateway.protocol.TdpOperation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplianceGatewayTest {

    @Test
    void auditsInboundAndOutboundTraffic() {
        InteropPipeline pipeline = InteropPipeline.createDefault();
        ObjectNode query = Jsons.objectOf("issuerId", "CN-1", "issuerEntityId", "ENT-1");
        ObjectNode metadata = Jsons.objectOf("idsParticipantId", "did:web:ids.example.eu:provider");
        pipeline.outboundTdp(TdpOperation.CATALOG_QUERY.code(), query, metadata);
        assertTrue(pipeline.complianceGateway().auditLog().size() >= 2);
        assertEquals("ALLOW", pipeline.complianceGateway().auditLog().get(0).decision());
    }

    @Test
    void sensitiveDataHookBlocksImportantDataExport() {
        InteropPipeline pipeline = InteropPipeline.createDefault();
        ObjectNode payload = Jsons.objectOf(
                "issuerId", "CN-1",
                "classification", "重要数据",
                "dataProductId", "DP-CN-ENERGY-0002"
        );
        ObjectNode metadata = Jsons.objectOf("idsParticipantId", "did:web:ids.example.eu:provider");
        ObjectNode response = pipeline.outboundTdp(TdpOperation.PRODUCT_DETAIL.code(), payload, metadata);
        assertEquals("1", response.get("status").asText());
        assertEquals("CB-CLASSIFICATION", response.get("code").asText());
        assertEquals("DENY", pipeline.complianceGateway().auditLog().get(0).decision());
    }

    @Test
    void destinationAllowlistIsPluggable() {
        InteropPipeline pipeline = InteropPipeline.createDefault();
        pipeline.complianceGateway().registerHook(new DestinationAllowlistHook(Set.of("JP")));
        ObjectNode query = Jsons.objectOf("issuerId", "CN-1", "issuerEntityId", "ENT-1");
        ObjectNode metadata = Jsons.objectOf(
                "idsParticipantId", "did:web:ids.example.eu:provider",
                "destinationCountry", "EU"
        );
        ObjectNode response = pipeline.outboundTdp(TdpOperation.CATALOG_QUERY.code(), query, metadata);
        assertEquals("CB-DESTINATION", response.get("code").asText());
        assertFalse(pipeline.complianceGateway().extensionPoints().isEmpty());
    }
}
