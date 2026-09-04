package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code semantic-codes.json} / overlay 的根文档。
 *
 * <p>三张独立码表：动作（授权使用 ↔ odrl:use）、运算符（11 ↔ odrl:lteq）、
 * 约束名（空间范围 ↔ odrl:spatial）。业务实例值不在此文件。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticCodesDocument {

    private String version;
    private SemanticTable actions = new SemanticTable();
    private SemanticTable operators = new SemanticTable();
    private SemanticTable constraints = new SemanticTable();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public SemanticTable getActions() {
        return actions;
    }

    public void setActions(SemanticTable actions) {
        this.actions = actions == null ? new SemanticTable() : actions;
    }

    public SemanticTable getOperators() {
        return operators;
    }

    public void setOperators(SemanticTable operators) {
        this.operators = operators == null ? new SemanticTable() : operators;
    }

    public SemanticTable getConstraints() {
        return constraints;
    }

    public void setConstraints(SemanticTable constraints) {
        this.constraints = constraints == null ? new SemanticTable() : constraints;
    }

    /** 三张表分别建索引，启动或 merge 后调用。 */
    public void index() {
        actions.index();
        operators.index();
        constraints.index();
    }

    /** overlay 按表合并；{@code null} overlay 视为无覆盖。 */
    public void merge(SemanticCodesDocument overlay) {
        if (overlay == null) {
            return;
        }
        actions.merge(overlay.actions);
        operators.merge(overlay.operators);
        constraints.merge(overlay.constraints);
    }
}

/**
 * {@code field-mappings.json} 根文档：输出骨架模板 + 命名绑定集合。
 *
 * <p>{@code templates} 是转换前的目标骨架（如 dataset / offer），绑定只填叶子。
 * {@code bindings} 的 key 由适配层按场景点名，例如 {@code productToDataset}。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class FieldMappingsDocument {

    private String version;
    private Map<String, JsonNode> templates = new LinkedHashMap<>();
    private Map<String, List<FieldBinding>> bindings = new LinkedHashMap<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, JsonNode> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, JsonNode> templates) {
        this.templates = templates == null ? new LinkedHashMap<>() : templates;
    }

    public Map<String, List<FieldBinding>> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, List<FieldBinding>> bindings) {
        this.bindings = bindings == null ? new LinkedHashMap<>() : bindings;
    }
}
