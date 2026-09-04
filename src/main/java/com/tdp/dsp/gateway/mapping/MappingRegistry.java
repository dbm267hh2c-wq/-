package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动时加载字段契约与语义码表。业务实例值不在此，只加载「怎么转」。
 */
public class MappingRegistry {

    public static final String SEMANTIC_RESOURCE = "mappings/semantic-codes.json";
    public static final String FIELD_RESOURCE = "mappings/field-mappings.json";
    public static final String OVERLAY_RESOURCE = "mappings/semantic-overlay.json";

    private final SemanticCodesDocument semantics;
    private final FieldMappingsDocument fields;

    public MappingRegistry(SemanticCodesDocument semantics, FieldMappingsDocument fields) {
        this.semantics = semantics;
        this.fields = fields;
        this.semantics.index();
    }

    public static MappingRegistry fromClasspath() {
        return fromClasspath(null);
    }

    public static MappingRegistry fromClasspath(String mappingDir) {
        SemanticCodesDocument semantics = readSemantic(SEMANTIC_RESOURCE);
        FieldMappingsDocument fields = readFields(FIELD_RESOURCE);
        MappingRegistry registry = new MappingRegistry(semantics, fields);
        registry.mergeSemantic(readOptionalSemantic(OVERLAY_RESOURCE));
        String extra = mappingDir;
        if (extra == null || extra.isBlank()) {
            extra = System.getProperty("tdp.dsp.mapping.dir", System.getenv("TDP_DSP_MAPPING_DIR"));
        }
        if (extra != null && !extra.isBlank()) {
            registry.mergeFromDirectory(Path.of(extra));
        }
        return registry;
    }

    public void mergeSemantic(SemanticCodesDocument overlay) {
        semantics.merge(overlay);
    }

    public void mergeFromDirectory(Path directory) {
        Path semanticOverlay = directory.resolve("semantic-overlay.json");
        if (Files.isRegularFile(semanticOverlay)) {
            try (InputStream input = Files.newInputStream(semanticOverlay)) {
                mergeSemantic(Jsons.MAPPER.readValue(input, SemanticCodesDocument.class));
            } catch (IOException ex) {
                throw new IllegalStateException("无法读取码表覆盖文件: " + semanticOverlay, ex);
            }
        }
        Path fieldOverlay = directory.resolve("field-mappings.json");
        if (Files.isRegularFile(fieldOverlay)) {
            try (InputStream input = Files.newInputStream(fieldOverlay)) {
                FieldMappingsDocument overlay = Jsons.MAPPER.readValue(input, FieldMappingsDocument.class);
                fields.getTemplates().putAll(overlay.getTemplates());
                overlay.getBindings().forEach((key, value) ->
                        fields.getBindings().merge(key, value, (left, right) -> {
                            left.addAll(right);
                            return left;
                        }));
            } catch (IOException ex) {
                throw new IllegalStateException("无法读取字段映射覆盖文件: " + fieldOverlay, ex);
            }
        }
    }

    public SemanticTable actions() {
        return semantics.getActions();
    }

    public SemanticTable operators() {
        return semantics.getOperators();
    }

    public SemanticTable constraints() {
        return semantics.getConstraints();
    }

    public ObjectNode template(String name) {
        JsonNode node = fields.getTemplates().get(name);
        if (node == null || node.isNull()) {
            return Jsons.object();
        }
        return node.deepCopy();
    }

    public List<FieldBinding> bindings(String name) {
        return fields.getBindings().getOrDefault(name, List.of());
    }

    public ObjectNode applyTdpToDsp(String bindingSet, JsonNode source, ObjectNode target) {
        return apply(bindingSet, source, target, true);
    }

    public ObjectNode applyDspToTdp(String bindingSet, JsonNode source, ObjectNode target) {
        return apply(bindingSet, source, target, false);
    }

    public ObjectNode apply(String bindingSet, JsonNode source, ObjectNode target, boolean tdpToDsp) {
        ObjectNode result = target == null ? Jsons.object() : target;
        for (FieldBinding binding : bindings(bindingSet)) {
            JsonNode value = FieldPath.read(source, binding.getFrom());
            if (value == null || value.isNull()) {
                if (binding.isRequired()) {
                    throw new IllegalArgumentException("缺少必填字段: " + binding.getFrom());
                }
                continue;
            }
            FieldPath.write(result, binding.getTo(), encode(binding.getCodec(), value, tdpToDsp));
        }
        return result;
    }

    public String actionToDsp(String tdp) {
        return actions().toDsp(tdp);
    }

    public String actionToTdp(String dsp) {
        return actions().toTdp(dsp);
    }

    public String operatorToDsp(String tdp) {
        return operators().toDsp(tdp);
    }

    public String operatorToTdp(String dsp) {
        return operators().toTdp(dsp);
    }

    public String constraintToDsp(String tdp) {
        return constraints().toDsp(tdp);
    }

    public String constraintToTdp(String dsp) {
        return constraints().toTdp(dsp);
    }

    public ObjectNode snapshot() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("actions", tableView(actions()));
        view.put("operators", tableView(operators()));
        view.put("constraints", tableView(constraints()));
        view.put("bindingSets", fields.getBindings().keySet());
        view.put("templates", fields.getTemplates().keySet());
        return Jsons.MAPPER.valueToTree(view);
    }

    private JsonNode encode(String codec, JsonNode value, boolean tdpToDsp) {
        if (codec == null || codec.isBlank()) {
            return value;
        }
        SemanticTable table = switch (codec) {
            case "action" -> actions();
            case "operator" -> operators();
            case "constraint" -> constraints();
            default -> throw new IllegalArgumentException("未知 codec: " + codec);
        };
        if (value.isArray()) {
            ArrayNode mapped = Jsons.array();
            value.forEach(item -> mapped.add(mapText(table, item.asText(), tdpToDsp)));
            return mapped;
        }
        return Jsons.MAPPER.getNodeFactory().textNode(mapText(table, value.asText(), tdpToDsp));
    }

    private static String mapText(SemanticTable table, String text, boolean tdpToDsp) {
        return tdpToDsp ? table.toDsp(text) : table.toTdp(text);
    }

    private static Map<String, Object> tableView(SemanticTable table) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("size", table.size());
        view.put("defaultTdp", table.getDefaultTdp());
        view.put("defaultDsp", table.getDefaultDsp());
        view.put("entries", table.getEntries());
        return view;
    }

    private static SemanticCodesDocument readSemantic(String resource) {
        try (InputStream input = resource(resource)) {
            return Jsons.MAPPER.readValue(input, SemanticCodesDocument.class);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载语义码表: " + resource, ex);
        }
    }

    private static FieldMappingsDocument readFields(String resource) {
        try (InputStream input = resource(resource)) {
            return Jsons.MAPPER.readValue(input, FieldMappingsDocument.class);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载字段映射: " + resource, ex);
        }
    }

    private static SemanticCodesDocument readOptionalSemantic(String resource) {
        InputStream input = MappingRegistry.class.getClassLoader().getResourceAsStream(resource);
        if (input == null) {
            return null;
        }
        try (input) {
            return Jsons.MAPPER.readValue(input, SemanticCodesDocument.class);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载语义码表覆盖: " + resource, ex);
        }
    }

    private static InputStream resource(String name) {
        InputStream input = MappingRegistry.class.getClassLoader().getResourceAsStream(name);
        if (input == null) {
            throw new IllegalStateException("缺少 classpath 资源: " + name);
        }
        return input;
    }
}
