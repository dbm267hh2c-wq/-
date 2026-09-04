package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public void index() {
        actions.index();
        operators.index();
        constraints.index();
    }

    public void merge(SemanticCodesDocument overlay) {
        if (overlay == null) {
            return;
        }
        actions.merge(overlay.actions);
        operators.merge(overlay.operators);
        constraints.merge(overlay.constraints);
    }
}

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
