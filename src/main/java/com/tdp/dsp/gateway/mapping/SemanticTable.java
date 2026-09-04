package com.tdp.dsp.gateway.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticTable {

    private String defaultTdp = "";
    private String defaultDsp = "";
    private boolean passThroughUnknown;
    private List<SemanticEntry> entries = new ArrayList<>();
    private final Map<String, String> tdpToDsp = new LinkedHashMap<>();
    private final Map<String, String> dspToTdp = new LinkedHashMap<>();

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

    private static String shortName(String value) {
        int index = value.lastIndexOf(':');
        return index >= 0 ? value.substring(index + 1) : value;
    }

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
