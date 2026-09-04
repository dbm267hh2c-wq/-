package com.tdp.dsp.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "tdp.dsp")
public class GatewayProperties {

    private String mappingDir = "";
    private List<String> destinationAllowlist = new ArrayList<>();
    private ParticipantProperties local = new ParticipantProperties();
    private ParticipantProperties overseas = new ParticipantProperties();

    public String getMappingDir() {
        return mappingDir;
    }

    public void setMappingDir(String mappingDir) {
        this.mappingDir = mappingDir;
    }

    public List<String> getDestinationAllowlist() {
        return destinationAllowlist;
    }

    public void setDestinationAllowlist(List<String> destinationAllowlist) {
        this.destinationAllowlist = destinationAllowlist;
    }

    public ParticipantProperties getLocal() {
        return local;
    }

    public void setLocal(ParticipantProperties local) {
        this.local = local;
    }

    public ParticipantProperties getOverseas() {
        return overseas;
    }

    public void setOverseas(ParticipantProperties overseas) {
        this.overseas = overseas;
    }

    public static class ParticipantProperties {
        private String tdpEntityId;
        private String tdpConnectorId;
        private String tdpConnectorName;
        private String idsParticipantId;
        private String idsConnectorUrl;
        private String region = "CN";
        private String dataspace = "tdp";

        public String getTdpEntityId() {
            return tdpEntityId;
        }

        public void setTdpEntityId(String tdpEntityId) {
            this.tdpEntityId = tdpEntityId;
        }

        public String getTdpConnectorId() {
            return tdpConnectorId;
        }

        public void setTdpConnectorId(String tdpConnectorId) {
            this.tdpConnectorId = tdpConnectorId;
        }

        public String getTdpConnectorName() {
            return tdpConnectorName;
        }

        public void setTdpConnectorName(String tdpConnectorName) {
            this.tdpConnectorName = tdpConnectorName;
        }

        public String getIdsParticipantId() {
            return idsParticipantId;
        }

        public void setIdsParticipantId(String idsParticipantId) {
            this.idsParticipantId = idsParticipantId;
        }

        public String getIdsConnectorUrl() {
            return idsConnectorUrl;
        }

        public void setIdsConnectorUrl(String idsConnectorUrl) {
            this.idsConnectorUrl = idsConnectorUrl;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getDataspace() {
            return dataspace;
        }

        public void setDataspace(String dataspace) {
            this.dataspace = dataspace;
        }
    }
}
