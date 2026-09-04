package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldBinding {

    private String from;
    private String to;
    private String codec;
    private boolean required;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<String> missingWhen(com.fasterxml.jackson.databind.JsonNode source) {
        List<String> missing = new ArrayList<>();
        if (required && FieldPath.read(source, from) == null) {
            missing.add(from);
        }
        return missing;
    }
}
