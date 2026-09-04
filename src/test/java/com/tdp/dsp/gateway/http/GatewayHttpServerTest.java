package com.tdp.dsp.gateway.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.model.dsp.DspMessages;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayHttpServerTest {

    private GatewayHttpServer server;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeEach
    void setUp() throws Exception {
        server = new GatewayHttpServer(InteropPipeline.createDefault(), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void healthAndExtensionEndpointsExposeFourLayers() throws Exception {
        JsonNode health = get("/health");
        assertEquals("UP", health.get("status").asText());
        JsonNode extensions = get("/compliance/extensions");
        assertTrue(extensions.get("registeredHooks").toString().contains("sensitive-data"));
        JsonNode participants = get("/bridge/participants");
        assertTrue(participants.get("participants").size() >= 2);
        JsonNode mappings = get("/mappings");
        assertTrue(mappings.get("actions").get("size").asInt() >= 7);
        assertTrue(mappings.get("bindingSets").toString().contains("productToDataset"));
    }

    @Test
    void inboundDspCatalogGoesThroughComplianceGateway() throws Exception {
        ObjectNode body = DspMessages.catalogRequest(java.util.List.of("交通"));
        HttpResponse<String> response = post("/dsp/catalog/request", body, null);
        assertEquals(200, response.statusCode());
        JsonNode catalog = Jsons.MAPPER.readTree(response.body());
        assertEquals("Catalog", Jsons.typeName(catalog));
        assertTrue(catalog.get("dcat:dataset").size() >= 1);
        JsonNode audit = get("/audit");
        assertTrue(audit.get("total").asInt() >= 2);
    }

    @Test
    void outboundTdpCatalogQueryReturnsIdsProducts() throws Exception {
        ObjectNode body = Jsons.objectOf(
                "issuerId", "CN-TDP-CONNECTOR-001",
                "issuerEntityId", "91310000MA1FL0XXXX"
        );
        HttpResponse<String> response = post("/tdp/catalogQuery", body, "did:web:ids.example.eu:provider");
        assertEquals(200, response.statusCode());
        JsonNode json = Jsons.MAPPER.readTree(response.body());
        assertEquals("EU Port Throughput", json.get("products").get(0).get("dataProductName").asText());
    }

    @Test
    void outboundImportantDataIsBlockedAtGateway() throws Exception {
        ObjectNode body = Jsons.objectOf(
                "issuerId", "CN-TDP-CONNECTOR-001",
                "classification", "重要数据",
                "dataProductId", "DP-CN-ENERGY-0002"
        );
        HttpResponse<String> response = post("/tdp/productDetail", body, "did:web:ids.example.eu:provider");
        assertEquals(403, response.statusCode());
        JsonNode json = Jsons.MAPPER.readTree(response.body());
        assertEquals("CB-CLASSIFICATION", json.get("code").asText());
    }

    @Test
    void outboundCatalogQueryRejectsPayloadMissingContractFields() throws Exception {
        ObjectNode body = Jsons.objectOf("keyword", "交通");
        HttpResponse<String> response = post("/tdp/catalogQuery", body, "did:web:ids.example.eu:provider");
        assertEquals(400, response.statusCode());
        JsonNode json = Jsons.MAPPER.readTree(response.body());
        assertEquals("SCHEMA_INVALID", json.get("code").asText());
    }

    private JsonNode get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return Jsons.MAPPER.readTree(response.body());
    }

    private HttpResponse<String> post(String path, ObjectNode body, String idsParticipant) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Jsons.MAPPER.writeValueAsString(body)));
        if (idsParticipant != null) {
            builder.header("X-IDS-Participant", idsParticipant);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.port() + path);
    }
}
