package com.tdp.dsp.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关运行时配置，绑定 {@code application.yml} 中的 {@code tdp.dsp} 前缀。
 *
 * <p>字段契约与语义码表默认从 classpath 加载；{@link #mappingDir} 可指向外部目录做覆盖，
 * 便于不改 Java 即可增补「联合建模」等动作码。
 */
@ConfigurationProperties(prefix = "tdp.dsp")
public class GatewayProperties {

    /**
     * 外部映射目录。目录内可放 {@code semantic-overlay.json}、{@code field-mappings.json}。
     * 为空时再看系统属性 {@code tdp.dsp.mapping.dir} 或环境变量 {@code TDP_DSP_MAPPING_DIR}。
     */
    private String mappingDir = "";

    /**
     * 出境目的国 / 目的空间白名单。空列表表示 {@code DestinationAllowlistHook} 尚未启用。
     */
    private List<String> destinationAllowlist = new ArrayList<>();

    /** 本侧（国内）连接器与主体身份。 */
    private ParticipantProperties local = new ParticipantProperties();

    /** 对端（境外 IDS）连接器与主体身份，启动时注册进桥接层目录。 */
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

    /**
     * 单个参与方的双边身份：国内实体/连接器 与 IDS Participant / Connector URL 一一对应。
     */
    public static class ParticipantProperties {

        /** 国内可信数据空间主体标识（如统一社会信用代码或空间内实体 ID）。 */
        private String tdpEntityId;

        /** 国内接入连接器 ID。 */
        private String tdpConnectorId;

        /** 国内连接器显示名，写入模拟产品策略的执行节点信息。 */
        private String tdpConnectorName;

        /** IDS 参与方 DID，例如 {@code did:web:ids.example.eu:provider}。 */
        private String idsParticipantId;

        /** IDS 连接器 HTTP 基址，出境时作为对端调用地址。 */
        private String idsConnectorUrl;

        /** 区域代码，默认 {@code CN}；境外示例为 {@code EU}。 */
        private String region = "CN";

        /**
         * 所属数据空间。国内侧为 {@code tdp}；桥接层用「非 tdp」识别默认可达的境外参与方。
         */
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
