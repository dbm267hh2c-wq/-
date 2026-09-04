package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.tdp.dsp.gateway.json.Jsons;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 出境国内报文的轻量 JSON Schema 校验。
 *
 * <p>只覆盖当前契约用到的子集：{@code type}、{@code required}、{@code minLength}、
 * {@code enum}、{@code minimum}、{@code minItems}、以及 {@code properties}/{@code items} 递归。
 * 完整 Draft 2020-12（$ref、oneOf、format 等）留给后续接入专业校验库。
 *
 * <p>Schema 文件放在 classpath {@code schema/}。{@code schemaFile} 为空则视为该操作不校验。
 */
public class ContractSchemaValidator {

    /**
     * @return 违反项列表；空列表表示通过。路径用 JSON Pointer 风格的 {@code $.field}
     */
    public List<String> validate(String schemaFile, JsonNode payload) {
        if (schemaFile == null || schemaFile.isBlank()) {
            return List.of();
        }
        JsonNode schema = load(schemaFile);
        List<String> errors = new ArrayList<>();
        validateNode(schema, payload, "$", errors);
        return errors;
    }

    public boolean isValid(String schemaFile, JsonNode payload) {
        return validate(schemaFile, payload).isEmpty();
    }

    private void validateNode(JsonNode schema, JsonNode payload, String path, List<String> errors) {
        if (schema == null) {
            return;
        }
        String type = text(schema, "type");
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            errors.add(path + " 不能为空");
            return;
        }
        if ("object".equals(type) && !payload.isObject()) {
            errors.add(path + " 应为对象");
            return;
        }
        if ("array".equals(type) && !payload.isArray()) {
            errors.add(path + " 应为数组");
            return;
        }
        if ("string".equals(type) && !payload.isTextual()) {
            errors.add(path + " 应为字符串");
            return;
        }
        if ("integer".equals(type) && !payload.isIntegralNumber()) {
            errors.add(path + " 应为整数");
            return;
        }
        if (payload.isTextual()) {
            int minLength = schema.path("minLength").asInt(0);
            if (payload.asText().length() < minLength) {
                errors.add(path + " 长度不足");
            }
            JsonNode enums = schema.get("enum");
            if (enums != null && enums.isArray()) {
                boolean matched = false;
                for (JsonNode item : enums) {
                    if (item.asText().equals(payload.asText())) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    errors.add(path + " 不在枚举范围内");
                }
            }
        }
        if (payload.isNumber()) {
            if (schema.has("minimum") && payload.asInt() < schema.get("minimum").asInt()) {
                errors.add(path + " 小于最小值");
            }
        }
        if (payload.isArray()) {
            int minItems = schema.path("minItems").asInt(0);
            if (payload.size() < minItems) {
                errors.add(path + " 数组元素不足");
            }
            JsonNode itemSchema = schema.get("items");
            if (itemSchema != null) {
                int index = 0;
                for (JsonNode item : payload) {
                    validateNode(itemSchema, item, path + "[" + index + "]", errors);
                    index++;
                }
            }
        }
        if (payload.isObject()) {
            JsonNode required = schema.get("required");
            if (required != null && required.isArray()) {
                for (JsonNode field : required) {
                    String name = field.asText();
                    if (!payload.has(name) || payload.get(name).isNull()) {
                        errors.add(path + "." + name + " 为必填");
                    }
                }
            }
            JsonNode properties = schema.get("properties");
            if (properties != null && properties.isObject()) {
                // 只校验 Schema 声明过的字段；额外字段放行，避免国标扩展字段被误杀
                properties.fields().forEachRemaining(entry -> {
                    if (payload.has(entry.getKey())) {
                        validateNode(entry.getValue(), payload.get(entry.getKey()), path + "." + entry.getKey(), errors);
                    }
                });
            }
        }
    }

    private JsonNode load(String schemaFile) {
        String resource = schemaFile.startsWith("schema/") ? schemaFile : "schema/" + schemaFile;
        try (InputStream input = ContractSchemaValidator.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("缺少 Schema 资源: " + resource);
            }
            return Jsons.MAPPER.readTree(input);
        } catch (IOException ex) {
            throw new IllegalStateException("无法读取 Schema: " + resource, ex);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
