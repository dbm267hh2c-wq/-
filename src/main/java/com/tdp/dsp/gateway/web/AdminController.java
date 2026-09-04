package com.tdp.dsp.gateway.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运维只读接口：健康检查、审计、参与方目录、合规扩展点、已加载码表。
 *
 * <p>不走跨境流水线，也不会改业务状态。{@code /mappings} 用于确认 overlay 是否生效。
 */
@RestController
public class AdminController {

    private final InteropPipeline pipeline;

    public AdminController(InteropPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /** 进程存活探测，并声明运行时与四层名称，便于部署探活。 */
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

    /** 合规关口内存审计（进程内有效，重启清空）。 */
    @GetMapping("/audit")
    public JsonNode audit() {
        return pipeline.complianceGateway().auditView();
    }

    /** 桥接层已注册的双边身份目录。 */
    @GetMapping("/bridge/participants")
    public JsonNode participants() {
        return pipeline.bridgeLayer().directory();
    }

    /** 列出预留合规扩展点及当前已注册钩子，供后续接入分类分级 / 出境评估时对照。 */
    @GetMapping("/compliance/extensions")
    public JsonNode extensions() {
        return Jsons.objectOf(
                "extensionPoints", pipeline.complianceGateway().extensionPoints(),
                "registeredHooks", pipeline.complianceGateway().hooks().stream().map(hook -> hook.name()).toList()
        );
    }

    /** 当前动作 / 运算符 / 约束码表快照，以及字段绑定集合名。 */
    @GetMapping("/mappings")
    public JsonNode mappings() {
        return pipeline.mappingRegistry().snapshot();
    }
}
