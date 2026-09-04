package com.tdp.dsp.gateway.mapping;

import com.tdp.dsp.gateway.json.Jsons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingRegistryTest {

    private final MappingRegistry registry = MappingRegistry.fromClasspath();

    @Test
    void loadsSemanticCodesFromClasspath() {
        assertEquals("odrl:lteq", registry.operatorToDsp("11"));
        assertEquals("11", registry.operatorToTdp("odrl:lteq"));
        assertEquals("odrl:use", registry.actionToDsp("授权使用"));
        assertEquals("脱敏", registry.actionToTdp("tdp:desensitize"));
        assertEquals("odrl:purpose", registry.constraintToDsp("使用目的"));
        assertEquals("空间范围", registry.constraintToTdp("odrl:spatial"));
        assertTrue(registry.actions().size() >= 7);
        assertFalse(registry.bindings("productToDataset").isEmpty());
    }

    @Test
    void overlayAddsCodesWithoutChangingJava() throws Exception {
        assertFalse(registry.actions().knowsTdp("联合建模"));
        SemanticCodesDocument overlay = Jsons.MAPPER.readValue(
                """
                {
                  "actions": {
                    "entries": [
                      { "tdp": "联合建模", "tdpAliases": ["federated"], "dsp": "tdp:jointModeling" }
                    ]
                  }
                }
                """,
                SemanticCodesDocument.class
        );
        MappingRegistry isolated = MappingRegistry.fromClasspath();
        isolated.mergeSemantic(overlay);
        assertEquals("tdp:jointModeling", isolated.actionToDsp("联合建模"));
        assertEquals("联合建模", isolated.actionToTdp("tdp:jointModeling"));
        assertEquals("tdp:jointModeling", isolated.actionToDsp("federated"));
    }
}
