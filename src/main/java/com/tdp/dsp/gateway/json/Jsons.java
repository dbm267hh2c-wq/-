package com.tdp.dsp.gateway.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public static String text(JsonNode node, String... keys) {
        JsonNode value = get(node, keys);
        return value == null || value.isNull() ? null : value.asText();
    }

    public static String textOrEmpty(JsonNode node, String... keys) {
        String value = text(node, keys);
        return value == null ? "" : value;
    }

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
