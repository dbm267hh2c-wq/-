package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 一张双向语义码表（动作、运算符或约束名）。
 *
 * <p>JSON 里写 {@code tdp}/{@code dsp} 及别名；{@link #index()} 后建成小写索引。
 * DSP 侧同时索引完整 IRI（{@code odrl:use}）与短名（{@code use}），兼容两种写法。
 *
 * <p>{@link #passThroughUnknown} 为真时未知码原样放行，便于灰度接入新码；
 * 默认为假，未知码回落到 {@code defaultTdp}/{@code defaultDsp}。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticTable {

    private String defaultTdp = "";
    private String defaultDsp = "";
    private boolean passThroughUnknown;
    private List<SemanticEntry> entries = new ArrayList<>();

    /** 规范化后的国内码 → DSP 码。含 tdp 别名。 */
    private final Map<String, String> tdpToDsp = new LinkedHashMap<>();

    /** 规范化后的 DSP 码 / 短名 / 别名 → 国内码。 */
    private final Map<String, String> dspToTdp = new LinkedHashMap<>();

    /**
     * 根据 entries 重建索引。merge overlay 后必须再调一次。
     */
    public void index() {
        tdpToDsp.clear();
        dspToTdp.clear();
        for (SemanticEntry entry : entries) {
            if (entry.getTdp() != null) {
                tdpToDsp.put(normalize(entry.getTdp()), entry.getDsp());
                for (String alias : entry.tdpAliases()) {
                    tdpToDsp.put(normalize(alias), entry.getDsp());
                }
            }
            if (entry.getDsp() != null) {
                dspToTdp.put(normalize(entry.getDsp()), entry.getTdp());
                dspToTdp.put(normalize(shortName(entry.getDsp())), entry.getTdp());
                for (String alias : entry.dspAliases()) {
                    dspToTdp.put(normalize(alias), entry.getTdp());
                    dspToTdp.put(normalize(shortName(alias)), entry.getTdp());
                }
            }
        }
    }

    /**
     * 追加 overlay 条目并覆盖默认值。后写入的同码会覆盖先前索引（后加载优先）。
     */
    public void merge(SemanticTable overlay) {
        if (overlay == null) {
            return;
        }
        entries.addAll(overlay.entries);
        if (overlay.defaultTdp != null && !overlay.defaultTdp.isBlank()) {
            defaultTdp = overlay.defaultTdp;
        }
        if (overlay.defaultDsp != null && !overlay.defaultDsp.isBlank()) {
            defaultDsp = overlay.defaultDsp;
        }
        index();
    }

    public String toDsp(String tdpCode) {
        if (tdpCode == null || tdpCode.isBlank()) {
            return defaultDsp;
        }
        String mapped = tdpToDsp.get(normalize(tdpCode));
        if (mapped != null) {
            return mapped;
        }
        return passThroughUnknown ? tdpCode : defaultDsp;
    }

    public String toTdp(String dspCode) {
        if (dspCode == null || dspCode.isBlank()) {
            return defaultTdp;
        }
        String mapped = dspToTdp.get(normalize(dspCode));
        if (mapped == null) {
            // 再试一次短名，应对索引时未拆前缀、运行时却带前缀的情况
            mapped = dspToTdp.get(normalize(shortName(dspCode)));
        }
        if (mapped != null) {
            return mapped;
        }
        return passThroughUnknown ? dspCode : defaultTdp;
    }

    public boolean knowsTdp(String tdpCode) {
        return tdpCode != null && tdpToDsp.containsKey(normalize(tdpCode));
    }

    public List<SemanticEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<SemanticEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : entries;
    }

    public String getDefaultTdp() {
        return defaultTdp;
    }

    public void setDefaultTdp(String defaultTdp) {
        this.defaultTdp = defaultTdp;
    }

    public String getDefaultDsp() {
        return defaultDsp;
    }

    public void setDefaultDsp(String defaultDsp) {
        this.defaultDsp = defaultDsp;
    }

    public boolean isPassThroughUnknown() {
        return passThroughUnknown;
    }

    public void setPassThroughUnknown(boolean passThroughUnknown) {
        this.passThroughUnknown = passThroughUnknown;
    }

    public int size() {
        return entries.size();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** 取冒号后最后一段，{@code odrl:lteq} → {@code lteq}。 */
    private static String shortName(String value) {
        int index = value.lastIndexOf(':');
        return index >= 0 ? value.substring(index + 1) : value;
    }

    /**
     * 一条码表记录。别名用于历史国标用词或 DSP 多种 IRI 写法。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SemanticEntry {
        private String tdp;
        private String dsp;
        private List<String> tdpAliases = List.of();
        private List<String> dspAliases = List.of();

        public String getTdp() {
            return tdp;
        }

        public void setTdp(String tdp) {
            this.tdp = tdp;
        }

        public String getDsp() {
            return dsp;
        }

        public void setDsp(String dsp) {
            this.dsp = dsp;
        }

        public List<String> tdpAliases() {
            return tdpAliases == null ? List.of() : tdpAliases;
        }

        public List<String> getTdpAliases() {
            return tdpAliases;
        }

        public void setTdpAliases(List<String> tdpAliases) {
            this.tdpAliases = tdpAliases;
        }

        public List<String> dspAliases() {
            return dspAliases == null ? List.of() : dspAliases;
        }

        public List<String> getDspAliases() {
            return dspAliases;
        }

        public void setDspAliases(List<String> dspAliases) {
            this.dspAliases = dspAliases;
        }
    }
}
