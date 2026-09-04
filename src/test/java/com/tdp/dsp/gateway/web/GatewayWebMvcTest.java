package com.tdp.dsp.gateway.web;

import com.tdp.dsp.gateway.model.dsp.DspMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthAndAdminEndpointsExposeSpringBootRuntime() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.runtime").value("spring-boot"))
                .andExpect(jsonPath("$.java").value("17"));
        mockMvc.perform(get("/compliance/extensions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredHooks[0]").value("sensitive-data"));
        mockMvc.perform(get("/bridge/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(greaterThanOrEqualTo(2)));
        mockMvc.perform(get("/mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions['size']").value(greaterThanOrEqualTo(7)))
                .andExpect(jsonPath("$.bindingSets", hasItem("productToDataset")));
    }

    @Test
    void inboundDspCatalogGoesThroughComplianceGateway() throws Exception {
        String body = DspMessages.catalogRequest(java.util.List.of("交通")).toString();
        mockMvc.perform(post("/dsp/catalog/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['@type']").value("dcat:Catalog"))
                .andExpect(jsonPath("$.['dcat:dataset'].length()").value(greaterThanOrEqualTo(1)));
        mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void outboundTdpCatalogQueryReturnsIdsProducts() throws Exception {
        mockMvc.perform(post("/tdp/catalogQuery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-IDS-Participant", "did:web:ids.example.eu:provider")
                        .content("""
                                {"issuerId":"CN-TDP-CONNECTOR-001","issuerEntityId":"91310000MA1FL0XXXX"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].dataProductName").value("EU Port Throughput"));
    }

    @Test
    void outboundImportantDataIsBlockedAtGateway() throws Exception {
        mockMvc.perform(post("/tdp/productDetail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-IDS-Participant", "did:web:ids.example.eu:provider")
                        .content("""
                                {"issuerId":"CN-TDP-CONNECTOR-001","classification":"重要数据","dataProductId":"DP-CN-ENERGY-0002"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CB-CLASSIFICATION"));
    }

    @Test
    void outboundCatalogQueryRejectsPayloadMissingContractFields() throws Exception {
        mockMvc.perform(post("/tdp/catalogQuery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-IDS-Participant", "did:web:ids.example.eu:provider")
                        .content("{\"keyword\":\"交通\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SCHEMA_INVALID"));
    }
}
