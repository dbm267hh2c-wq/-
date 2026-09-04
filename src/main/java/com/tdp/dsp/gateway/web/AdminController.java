package com.tdp.dsp.gateway.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminController {

    private final InteropPipeline pipeline;

    public AdminController(InteropPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @GetMapping("/health")
    public JsonNode health() {
        return Jsons.objectOf(
                "status", "UP",
                "service", "tdp-dsp-gateway",
                "runtime", "spring-boot",
                "java", "17",
                "layers", List.of("protocol", "adapter", "bridge", "compliance")
        );
    }

    @GetMapping("/audit")
    public JsonNode audit() {
        return pipeline.complianceGateway().auditView();
    }

    @GetMapping("/bridge/participants")
    public JsonNode participants() {
        return pipeline.bridgeLayer().directory();
    }

    @GetMapping("/compliance/extensions")
    public JsonNode extensions() {
        return Jsons.objectOf(
                "extensionPoints", pipeline.complianceGateway().extensionPoints(),
                "registeredHooks", pipeline.complianceGateway().hooks().stream().map(hook -> hook.name()).toList()
        );
    }

    @GetMapping("/mappings")
    public JsonNode mappings() {
        return pipeline.mappingRegistry().snapshot();
    }
}
