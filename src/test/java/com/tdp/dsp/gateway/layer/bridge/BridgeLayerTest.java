package com.tdp.dsp.gateway.layer.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.constant.ProtocolConstants;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.layer.adapter.MessageAdapter;
import com.tdp.dsp.gateway.layer.protocol.ProtocolConverter;
import com.tdp.dsp.gateway.model.common.Direction;
import com.tdp.dsp.gateway.model.common.InteropEnvelope;
import com.tdp.dsp.gateway.model.common.Participant;
import com.tdp.dsp.gateway.model.dsp.DspMessages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeLayerTest {

    @Test
    void inboundCatalogBridgesIdsConsumerToDomesticPlatform() {
        BridgeLayer bridge = newBridge();
        ObjectNode dsp = DspMessages.catalogRequest(java.util.List.of("交通"));
        InteropEnvelope envelope = new InteropEnvelope(
                Direction.INBOUND, "DSP", "TDP", ProtocolConstants.DSP_CATALOG_REQUEST, dsp, Jsons.object()
        );
        ObjectNode catalog = bridge.dispatch(envelope);
        assertEquals("Catalog", Jsons.typeName(catalog));
        assertTrue(catalog.get("dcat:dataset").size() >= 1);
        assertEquals("城市交通流量数据产品", catalog.get("dcat:dataset").get(0).get("dct:title").asText());
    }

    @Test
    void outboundCatalogBridgesDomesticConnectorToIds() {
        BridgeLayer bridge = newBridge();
        ObjectNode query = Jsons.objectOf(
                "issuerId", "CN-TDP-CONNECTOR-001",
                "issuerEntityId", "91310000MA1FL0XXXX"
        );
        ObjectNode metadata = Jsons.objectOf("idsParticipantId", "did:web:ids.example.eu:provider");
        InteropEnvelope envelope = new InteropEnvelope(
                Direction.OUTBOUND, "TDP", "DSP", ProtocolConstants.OP_CATALOG_QUERY, query, metadata
        );
        ObjectNode response = bridge.dispatch(envelope);
        assertEquals("0", response.get("status").asText());
        assertEquals("EU Port Throughput", response.get("products").get(0).get("dataProductName").asText());
    }

    @Test
    void inboundContractNegotiationCreatesDomesticContract() {
        BridgeLayer bridge = newBridge();
        ObjectNode offer = Jsons.object();
        offer.put("odrl:target", "DP-CN-TRAFFIC-0001");
        offer.set("odrl:permission", Jsons.array().add(Jsons.objectOf("odrl:action", "odrl:use")));
        ObjectNode dsp = DspMessages.contractRequest(
                "urn:uuid:eu-consumer",
                offer,
                "https://ids.example.eu/callback",
                "DP-CN-TRAFFIC-0001"
        );
        ObjectNode result = bridge.dispatch(new InteropEnvelope(
                Direction.INBOUND, "DSP", "TDP", ProtocolConstants.DSP_CONTRACT_REQUEST, dsp, Jsons.object()
        ));
        assertEquals("ContractNegotiation", Jsons.typeName(result));
        assertEquals("dspace:REQUESTED", result.get("dspace:state").asText());
        assertTrue(result.get("dspace:providerPid").asText().length() > 10);
    }

    private static BridgeLayer newBridge() {
        Participant local = new Participant(
                "91310000MA1FL0XXXX",
                "CN-TDP-CONNECTOR-001",
                "国内连接器",
                "did:web:tdp.example.cn:provider",
                "https://tdp.example.cn/connector"
        );
        Participant overseas = new Participant(
                "EU-IDS-PROVIDER-001",
                "EU-IDS-CONNECTOR-001",
                "IDS Connector EU",
                "did:web:ids.example.eu:provider",
                "https://ids.example.eu/connector",
                "EU",
                "ids"
        );
        MessageAdapter adapter = new MessageAdapter();
        ProtocolConverter converter = new ProtocolConverter(adapter);
        BridgeLayer bridge = new BridgeLayer(
                new InMemoryTdpPlatformClient(local),
                new InMemoryIdsConnectorClient(),
                converter,
                adapter
        );
        bridge.register(overseas);
        return bridge;
    }
}
