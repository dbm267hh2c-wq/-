package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;

import java.util.ArrayList;
import java.util.List;

/**
 * 点分路径读写 JSON，供字段契约 {@code from}/{@code to} 使用。
 *
 * <p>分隔符只有英文点 {@code .}。冒号属于 JSON-LD 字段名的一部分，例如
 * {@code dcat:distribution.0.dct:format} 拆成三段：{@code dcat:distribution}、
 * {@code 0}、{@code dct:format}，绝不能按冒号切开。
 *
 * <p>纯数字段视为数组下标。{@link #write} 会沿路径自动创建缺失的对象或数组。
 */
public final class FieldPath {

    private FieldPath() {
    }

    /**
     * 沿路径读取。任一节点缺失或下标越界则返回 {@code null}，调用方据此判断是否必填失败。
     */
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
                // 允许 dspace:filter 与 filter 互认，兼容紧凑 JSON-LD 与无前缀写法
                current = Jsons.get(current, segment);
            }
        }
        return current;
    }

    /**
     * 沿路径写入。中间节点按「下一段是否为下标」决定创建数组还是对象。
     */
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

    /**
     * 只按点拆分，保留段内冒号。空段也会保留，以便发现配置写错（如连续两点）。
     */
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

    /**
     * 取出或创建下一层节点。若已有节点类型与预期不符（对象/数组），用新节点覆盖，避免脏数据卡住映射。
     */
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

    /** 整段皆为十进制数字才当下标，避免把 {@code 11} 这种运算符码误判（运算符走字段值，不走路径段）。 */
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
