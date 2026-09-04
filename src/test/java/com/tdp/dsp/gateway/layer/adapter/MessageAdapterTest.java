package com.tdp.dsp.gateway.layer.adapter;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageAdapterTest {

    private final MessageAdapter adapter = new MessageAdapter();

    @Test
    void mapsDomesticProductFieldsToDcatDataset() {
        ObjectNode product = sampleProduct();
        ObjectNode dataset = adapter.productToDataset(product);
        assertEquals("DP-CN-TRAFFIC-0001", dataset.get("@id").asText());
        assertEquals("dcat:Dataset", dataset.get("@type").asText());
        assertEquals("城市交通流量数据产品", dataset.get("dct:title").asText());
        assertEquals("odrl:use", dataset.get("odrl:hasPolicy").get(0).get("odrl:permission").get(0).get("odrl:action").asText());
        assertEquals("odrl:purpose", dataset.get("odrl:hasPolicy").get(0)
                .get("odrl:permission").get(0)
                .get("odrl:constraint").get(0)
                .get("odrl:leftOperand").asText());
    }

    @Test
    void roundTripsProductAndDatasetSemantics() {
        ObjectNode original = sampleProduct();
        ObjectNode restored = adapter.datasetToProduct(adapter.productToDataset(original));
        assertEquals(original.get("dataProductId").asText(), restored.get("dataProductId").asText());
        assertEquals(original.get("dataProductName").asText(), restored.get("dataProductName").asText());
        assertTrue(Jsons.stringList(restored.get("strategy").get("actions")).contains("授权使用"));
        assertEquals("使用目的", restored.get("strategy").get("constraints").get(0).get("constraintName").asText());
        assertEquals("01", restored.get("strategy").get("constraints").get(0).get("constraintOperator").asText());
    }

    @Test
    void convertsFilterAndCatalogQuery() {
        ObjectNode query = Jsons.objectOf("issuerId", "CN-1", "issuerEntityId", "ENT-1", "keyword", "交通");
        ObjectNode dsp = adapter.catalogQueryToDspFilter(query);
        assertEquals("交通", dsp.get("dspace:filter").get(0).asText());
        ObjectNode back = adapter.dspFilterToCatalogQuery(dsp, "CN-1", "ENT-1");
        assertEquals("交通", back.get("keyword").asText());
    }

    @Test
    void mapsOperatorsAndActions() {
        assertEquals("odrl:lteq", adapter.mapOperatorToOdrl("11"));
        assertEquals("11", adapter.mapOperatorToTdp("odrl:lteq"));
        assertEquals("odrl:anonymize", adapter.mapActionToOdrl("匿名化"));
        assertEquals("脱敏", adapter.mapActionToTdp("tdp:desensitize"));
    }

    private static ObjectNode sampleProduct() {
        ObjectNode product = Jsons.object();
        product.put("dataProductId", "DP-CN-TRAFFIC-0001");
        product.put("dataProductName", "城市交通流量数据产品");
        product.put("dataProductAbstract", "道路断面流量");
        product.put("ownerEntityId", "91310000MA1FL0XXXX");
        ArrayNode keywords = Jsons.array();
        keywords.add("交通");
        product.set("keywords", keywords);
        product.put("format", "CSV");
        product.put("endpointUrl", "https://tdp.example.cn/connector");
        ObjectNode strategy = Jsons.object();
        ArrayNode actions = Jsons.array();
        actions.add("授权使用");
        strategy.set("actions", actions);
        ArrayNode constraints = Jsons.array();
        constraints.add(Jsons.objectOf(
                "constraintName", "使用目的",
                "constraintOperator", "01",
                "constraintValue", "科研"
        ));
        strategy.set("constraints", constraints);
        product.set("strategy", strategy);
        return product;
    }
}
