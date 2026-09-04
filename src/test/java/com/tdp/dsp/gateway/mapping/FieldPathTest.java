package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FieldPathTest {

    @Test
    void readsAndWritesJsonLdDottedPaths() {
        ObjectNode root = Jsons.object();
        FieldPath.write(root, "dcat:distribution.0.dct:format", Jsons.MAPPER.getNodeFactory().textNode("CSV"));
        FieldPath.write(root, "dct:description.0.@value", Jsons.MAPPER.getNodeFactory().textNode("简介"));
        assertEquals("CSV", FieldPath.read(root, "dcat:distribution.0.dct:format").asText());
        assertEquals("简介", FieldPath.read(root, "dct:description.0.@value").asText());
        assertNull(FieldPath.read(root, "missing.0.value"));
    }
}
