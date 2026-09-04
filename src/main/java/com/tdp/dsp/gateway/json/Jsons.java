package com.tdp.dsp.gateway.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * JSON 读写小工具，专门照顾 DSP JSON-LD 的前缀字段。
 *
 * <p>{@link #get(JsonNode, String...)} 按候选键依次查找，并把 {@code dspace:filter}
 * 与 {@code filter} 视为同一短名，避免紧凑上下文展开前后字段对不上。
 */
public final class Jsons {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private Jsons() {
    }

    public static ObjectNode object() {
        return MAPPER.createObjectNode();
    }

    public static ArrayNode array() {
        return MAPPER.createArrayNode();
    }

    /**
     * 按 key/value 交替参数建对象。值为 {@code null} 的键会被跳过。
     */
    public static ObjectNode objectOf(Object... keyValues) {
        ObjectNode node = object();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = String.valueOf(keyValues[i]);
            Object value = keyValues[i + 1];
            put(node, key, value);
        }
        return node;
    }

    public static void put(ObjectNode node, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof JsonNode jsonNode) {
            node.set(key, jsonNode);
        } else if (value instanceof String text) {
            node.put(key, text);
        } else if (value instanceof Integer number) {
            node.put(key, number);
        } else if (value instanceof Long number) {
            node.put(key, number);
        } else if (value instanceof Boolean flag) {
            node.put(key, flag);
        } else {
            node.set(key, MAPPER.valueToTree(value));
        }
    }

    /**
     * 按候选键取文本；都不存在则 {@code null}（与空字符串区分，便于「字段未出现」判断）。
     */
    public static String text(JsonNode node, String... keys) {
        JsonNode value = get(node, keys);
        return value == null || value.isNull() ? null : value.asText();
    }

    public static String textOrEmpty(JsonNode node, String... keys) {
        String value = text(node, keys);
        return value == null ? "" : value;
    }

    /**
     * 先精确匹配键，再按 JSON-LD 短名匹配。多个候选键表示「这个语义可能出现的几种写法」。
     */
    public static JsonNode get(JsonNode node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            if (node.has(key)) {
                return node.get(key);
            }
            String shortName = shortName(key);
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (shortName(field).equals(shortName)) {
                    return node.get(field);
                }
            }
        }
        return null;
    }

    /**
     * 取 {@code @type} 短名。数组形态取第一项，兼容 JSON-LD 多类型写法。
     */
    public static String typeName(JsonNode node) {
        JsonNode type = get(node, "@type");
        if (type == null || type.isNull()) {
            return "";
        }
        if (type.isArray() && !type.isEmpty()) {
            return shortName(type.get(0).asText());
        }
        return shortName(type.asText());
    }

    /** {@code dspace:CatalogRequestMessage} → {@code CatalogRequestMessage}。 */
    public static String shortName(String value) {
        if (value == null) {
            return "";
        }
        int index = value.lastIndexOf(':');
        return index >= 0 ? value.substring(index + 1) : value;
    }

    public static List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || node.isNull()) {
            return values;
        }
        if (node.isArray()) {
            node.forEach(item -> values.add(item.asText()));
        } else {
            values.add(node.asText());
        }
        return values;
    }

    public static ObjectNode requireObject(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            return objectNode;
        }
        throw new IllegalArgumentException("期望 JSON 对象，实际为: " + (node == null ? "null" : node.getNodeType()));
    }
}
