package com.tdp.dsp.gateway.layer.protocol;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.layer.adapter.MessageAdapter;
import com.tdp.dsp.gateway.model.dsp.DspMessages;
import com.tdp.dsp.gateway.protocol.DspMessageType;
import com.tdp.dsp.gateway.protocol.TdpOperation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolConverterTest {

    private final ProtocolConverter converter = new ProtocolConverter(new MessageAdapter());

    @Test
    void mapsDomesticOperationsToDspTypesAndPaths() {
        assertEquals(DspMessageType.CATALOG_REQUEST.typeName(),
                converter.tdpOperationToDspType(TdpOperation.CATALOG_QUERY.code()));
        assertEquals(DspMessageType.CONTRACT_REQUEST.dspPath(),
                converter.dspPathForTdpOperation(TdpOperation.CONTRACT_CREATE.code()));
        assertEquals(TdpOperation.CATALOG_QUERY.domesticPath(),
                converter.tdpPathForDspType("dspace:CatalogRequestMessage"));
        assertEquals(TdpOperation.CONTRACT_EXECUTION.code(),
                converter.dspTypeToTdpOperation("TransferRequestMessage"));
    }

    @Test
    void convertsCatalogQueryToCatalogRequestMessage() {
        ObjectNode query = Jsons.objectOf(
                "issuerId", "CN-TDP-CONNECTOR-001",
                "issuerEntityId", "91310000MA1FL0XXXX",
                "keyword", "交通"
        );
        ObjectNode dsp = converter.tdpToDsp(TdpOperation.CATALOG_QUERY.code(), query, Jsons.object());
        assertEquals("CatalogRequestMessage", Jsons.typeName(dsp));
        assertEquals("交通", dsp.get("dspace:filter").get(0).asText());
    }

    @Test
    void convertsContractRequestMessageToDomesticContractCreate() {
        ObjectNode offer = Jsons.object();
        offer.put("@type", "odrl:Offer");
        offer.put("odrl:target", "DP-CN-TRAFFIC-0001");
        offer.set("odrl:permission", Jsons.array().add(Jsons.objectOf("odrl:action", "odrl:use")));
        ObjectNode dsp = DspMessages.contractRequest(
                "urn:uuid:consumer",
                offer,
                "https://ids.example.eu/callback",
                "DP-CN-TRAFFIC-0001"
        );
        ObjectNode context = Jsons.objectOf(
                "tdpConnectorId", "CN-TDP-CONNECTOR-001",
                "tdpEntityId", "91310000MA1FL0XXXX"
        );
        ObjectNode tdp = converter.dspToTdp(dsp, context);
        assertEquals("02", tdp.get("signMode").asText());
        assertEquals("DP-CN-TRAFFIC-0001", tdp.get("strategy").get("subjectInfo").get("dataProductId").asText());
        assertTrue(tdp.get("contractName").asText().contains("跨境"));
    }

    @Test
    void convertsTdpCatalogIntoDcatCatalog() {
        ObjectNode products = Jsons.object();
        products.set("products", Jsons.array().add(Jsons.objectOf(
                "dataProductId", "DP-1",
                "dataProductName", "样例产品",
                "dataProductAbstract", "简介",
                "ownerEntityId", "ENT",
                "format", "JSON",
                "endpointUrl", "https://tdp.example.cn/connector"
        )));
        ObjectNode catalog = converter.tdpCatalogToDsp(products, "did:web:tdp.example.cn:provider",
                "https://tdp.example.cn/connector");
        assertEquals("Catalog", Jsons.typeName(catalog));
        assertEquals(1, catalog.get("dcat:dataset").size());
    }
}
