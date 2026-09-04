package com.tdp.dsp.gateway.pipeline;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.layer.adapter.MessageAdapter;
import com.tdp.dsp.gateway.layer.bridge.BridgeLayer;
import com.tdp.dsp.gateway.layer.bridge.IdsConnectorClient;
import com.tdp.dsp.gateway.layer.bridge.InMemoryIdsConnectorClient;
import com.tdp.dsp.gateway.layer.bridge.InMemoryTdpPlatformClient;
import com.tdp.dsp.gateway.layer.bridge.TdpPlatformClient;
import com.tdp.dsp.gateway.layer.compliance.ComplianceGateway;
import com.tdp.dsp.gateway.layer.compliance.DestinationAllowlistHook;
import com.tdp.dsp.gateway.layer.compliance.SensitiveDataHook;
import com.tdp.dsp.gateway.layer.protocol.ProtocolConverter;
import com.tdp.dsp.gateway.mapping.ContractSchemaValidator;
import com.tdp.dsp.gateway.mapping.MappingRegistry;
import com.tdp.dsp.gateway.model.common.Direction;
import com.tdp.dsp.gateway.model.common.InteropEnvelope;
import com.tdp.dsp.gateway.model.common.Participant;

import java.util.Set;

/**
 * 四层互联互通的门面：HTTP 与单测都只通过本类进出。
 *
 * <p>实际调用顺序固定为「④ 合规关口 → ③ 桥接 → ① 协议转换 + ② 消息适配」。
 * 本类负责把方向、协议名、操作码打进 {@link InteropEnvelope}，不自己做字段映射。
 *
 * <p>{@link #createDefault()} 给无 Spring 的单元测试用，参与方与钩子与生产默认配置对齐。
 */
public final class InteropPipeline {

    private final MessageAdapter messageAdapter;
    private final ProtocolConverter protocolConverter;
    private final BridgeLayer bridgeLayer;
    private final ComplianceGateway complianceGateway;
    private final MappingRegistry mappingRegistry;
    private final ContractSchemaValidator schemaValidator;

    public InteropPipeline(
            MessageAdapter messageAdapter,
            ProtocolConverter protocolConverter,
            BridgeLayer bridgeLayer,
            ComplianceGateway complianceGateway,
            MappingRegistry mappingRegistry,
            ContractSchemaValidator schemaValidator
    ) {
        this.messageAdapter = messageAdapter;
        this.protocolConverter = protocolConverter;
        this.bridgeLayer = bridgeLayer;
        this.complianceGateway = complianceGateway;
        this.mappingRegistry = mappingRegistry;
        this.schemaValidator = schemaValidator;
    }

    /**
     * 无容器装配：内存 TDP / IDS、默认码表、敏感数据钩子；目的地白名单为空（未启用）。
     */
    public static InteropPipeline createDefault() {
        Participant local = new Participant(
                "91310000MA1FL0XXXX",
                "CN-TDP-CONNECTOR-001",
                "国内可信数据空间接入连接器",
                "did:web:tdp.example.cn:provider",
                "https://tdp.example.cn/connector"
        );
        Participant overseas = new Participant(
                "EU-IDS-PROVIDER-001",
                "EU-IDS-CONNECTOR-001",
                "IDS Connector EU",
                "did:web:ids.example.eu:provider",
                "https://ids.example.eu/connector",
                "EU",
                "ids"
        );
        MappingRegistry mappings = MappingRegistry.fromClasspath();
        MessageAdapter adapter = new MessageAdapter(mappings);
        ProtocolConverter converter = new ProtocolConverter(adapter);
        TdpPlatformClient tdp = new InMemoryTdpPlatformClient(local);
        IdsConnectorClient ids = new InMemoryIdsConnectorClient();
        BridgeLayer bridge = new BridgeLayer(tdp, ids, converter, adapter);
        bridge.register(overseas);
        ComplianceGateway gateway = new ComplianceGateway(bridge);
        gateway.registerHook(new SensitiveDataHook());
        gateway.registerHook(new DestinationAllowlistHook(Set.of()));
        return new InteropPipeline(adapter, converter, bridge, gateway, mappings, new ContractSchemaValidator());
    }

    /**
     * 入境：DSP → 国内。{@code operation} 为 DSP {@code @type} 短名，例如 {@code CatalogRequestMessage}。
     *
     * @param dspMessage 境外原始 JSON-LD
     * @param metadata   请求头抽出的对端身份等，可空
     * @return DSP 形态响应；合规拒绝时为 {@code CatalogError}
     */
    public ObjectNode inboundDsp(String operation, ObjectNode dspMessage, ObjectNode metadata) {
        InteropEnvelope envelope = new InteropEnvelope(
                Direction.INBOUND,
                "DSP",
                "TDP",
                operation,
                dspMessage,
                metadata == null ? Jsons.object() : metadata
        );
        return complianceGateway.handle(envelope);
    }

    /**
     * 出境：国内 → DSP。{@code operation} 为国标操作码，例如 {@code catalogQuery}。
     *
     * @param tdpMessage 国内原始 JSON
     * @return 国内形态响应；合规拒绝时 {@code status=1}
     */
    public ObjectNode outboundTdp(String operation, ObjectNode tdpMessage, ObjectNode metadata) {
        InteropEnvelope envelope = new InteropEnvelope(
                Direction.OUTBOUND,
                "TDP",
                "DSP",
                operation,
                tdpMessage,
                metadata == null ? Jsons.object() : metadata
        );
        return complianceGateway.handle(envelope);
    }

    public MessageAdapter messageAdapter() {
        return messageAdapter;
    }

    public ProtocolConverter protocolConverter() {
        return protocolConverter;
    }

    public BridgeLayer bridgeLayer() {
        return bridgeLayer;
    }

    public ComplianceGateway complianceGateway() {
        return complianceGateway;
    }

    public MappingRegistry mappingRegistry() {
        return mappingRegistry;
    }

    public ContractSchemaValidator schemaValidator() {
        return schemaValidator;
    }
}
