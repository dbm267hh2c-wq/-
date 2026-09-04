package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;

import java.util.ArrayList;
import java.util.List;

/**
 * 点分路径读写，例如 {@code dcat:distribution.0.dct:format}。冒号属于 JSON-LD 字段名，不是分隔符。
 */
public final class FieldPath {

    private FieldPath() {
    }

    public static JsonNode read(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode current = root;
        for (String segment : split(path)) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }
            if (isIndex(segment)) {
                int index = Integer.parseInt(segment);
                if (!current.isArray() || index >= current.size()) {
                    return null;
                }
                current = current.get(index);
            } else {
                current = Jsons.get(current, segment);
            }
        }
        return current;
    }

    public static void write(ObjectNode root, String path, JsonNode value) {
        if (root == null || path == null || path.isBlank() || value == null || value.isNull()) {
            return;
        }
        List<String> segments = split(path);
        JsonNode cursor = root;
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            boolean last = i == segments.size() - 1;
            String next = last ? null : segments.get(i + 1);
            if (last) {
                setChild(cursor, segment, value);
                return;
            }
            cursor = childForWrite(cursor, segment, isIndex(next));
        }
    }

    public static List<String> split(String path) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '.') {
                segments.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        segments.add(current.toString());
        return segments;
    }

    private static JsonNode childForWrite(JsonNode parent, String segment, boolean childIsIndex) {
        if (isIndex(segment)) {
            ArrayNode array = ensureArray(parent);
            int index = Integer.parseInt(segment);
            while (array.size() <= index) {
                array.add(childIsIndex ? Jsons.array() : Jsons.object());
            }
            JsonNode existing = array.get(index);
            if (existing == null || existing.isNull() || existing.isMissingNode()
                    || (childIsIndex && !existing.isArray())
                    || (!childIsIndex && !existing.isObject())) {
                JsonNode created = childIsIndex ? Jsons.array() : Jsons.object();
                array.set(index, created);
                return created;
            }
            return existing;
        }
        ObjectNode object = ensureObject(parent);
        JsonNode existing = object.get(segment);
        if (existing == null || existing.isNull()
                || (childIsIndex && !existing.isArray())
                || (!childIsIndex && !existing.isObject())) {
            JsonNode created = childIsIndex ? Jsons.array() : Jsons.object();
            object.set(segment, created);
            return created;
        }
        return existing;
    }

    private static void setChild(JsonNode parent, String segment, JsonNode value) {
        if (isIndex(segment)) {
            ArrayNode array = ensureArray(parent);
            int index = Integer.parseInt(segment);
            while (array.size() <= index) {
                array.addNull();
            }
            array.set(index, value);
            return;
        }
        ensureObject(parent).set(segment, value);
    }

    private static ObjectNode ensureObject(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            return objectNode;
        }
        throw new IllegalStateException("路径父节点不是对象: " + node);
    }

    private static ArrayNode ensureArray(JsonNode node) {
        if (node instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        throw new IllegalStateException("路径父节点不是数组: " + node);
    }

    private static boolean isIndex(String segment) {
        if (segment == null || segment.isEmpty()) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            if (!Character.isDigit(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
