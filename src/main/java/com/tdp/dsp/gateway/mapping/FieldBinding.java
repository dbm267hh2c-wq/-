package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条字段映射：从源路径读值，可选经语义 codec 转码后写入目标路径。
 *
 * <p>对应 {@code field-mappings.json} 里某个 binding 集合中的一项。
 * {@code codec} 取值 {@code action} / {@code operator} / {@code constraint}，空则原样拷贝。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldBinding {

    /** 源点分路径，例如 {@code dataProductName} 或 {@code odrl:leftOperand}。 */
    private String from;

    /** 目标点分路径。 */
    private String to;

    /** 语义码表名称；空表示不做码值转换。 */
    private String codec;

    /** 源路径读不到值时是否视为契约失败。 */
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

    /**
     * 预检查：必填路径在源报文中不存在时返回缺失路径列表。
     * 运行时真正抛错在 {@link MappingRegistry#apply}，本方法给单测或预校验用。
     */
    public List<String> missingWhen(com.fasterxml.jackson.databind.JsonNode source) {
        List<String> missing = new ArrayList<>();
        if (required && FieldPath.read(source, from) == null) {
            missing.add(from);
        }
        return missing;
    }
}
