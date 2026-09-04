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
import com.tdp.dsp.gateway.model.common.Direction;
import com.tdp.dsp.gateway.model.common.InteropEnvelope;
import com.tdp.dsp.gateway.model.common.Participant;

import java.util.Set;

/**
 * 组装四层：合规关口 → 桥接 → 消息适配 / 协议转换。
 */
public final class InteropPipeline {

    private final MessageAdapter messageAdapter;
    private final ProtocolConverter protocolConverter;
    private final BridgeLayer bridgeLayer;
    private final ComplianceGateway complianceGateway;

    public InteropPipeline(
            MessageAdapter messageAdapter,
            ProtocolConverter protocolConverter,
            BridgeLayer bridgeLayer,
            ComplianceGateway complianceGateway
    ) {
        this.messageAdapter = messageAdapter;
        this.protocolConverter = protocolConverter;
        this.bridgeLayer = bridgeLayer;
        this.complianceGateway = complianceGateway;
    }

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
        MessageAdapter adapter = new MessageAdapter();
        ProtocolConverter converter = new ProtocolConverter(adapter);
        TdpPlatformClient tdp = new InMemoryTdpPlatformClient(local);
        IdsConnectorClient ids = new InMemoryIdsConnectorClient();
        BridgeLayer bridge = new BridgeLayer(tdp, ids, converter, adapter);
        bridge.register(overseas);
        ComplianceGateway gateway = new ComplianceGateway(bridge);
        gateway.registerHook(new SensitiveDataHook());
        gateway.registerHook(new DestinationAllowlistHook(Set.of()));
        return new InteropPipeline(adapter, converter, bridge, gateway);
    }

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
}
