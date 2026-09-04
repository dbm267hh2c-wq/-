package com.tdp.dsp.gateway.config;

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
import com.tdp.dsp.gateway.model.common.Participant;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;

@Configuration
public class GatewayConfiguration {

    @Bean
    public MappingRegistry mappingRegistry(GatewayProperties properties) {
        return MappingRegistry.fromClasspath(properties.getMappingDir());
    }

    @Bean
    public MessageAdapter messageAdapter(MappingRegistry mappingRegistry) {
        return new MessageAdapter(mappingRegistry);
    }

    @Bean
    public ProtocolConverter protocolConverter(MessageAdapter messageAdapter) {
        return new ProtocolConverter(messageAdapter);
    }

    @Bean
    public Participant localParticipant(GatewayProperties properties) {
        return toParticipant(properties.getLocal());
    }

    @Bean
    public Participant overseasParticipant(GatewayProperties properties) {
        return toParticipant(properties.getOverseas());
    }

    @Bean
    public TdpPlatformClient tdpPlatformClient(Participant localParticipant) {
        return new InMemoryTdpPlatformClient(localParticipant);
    }

    @Bean
    public IdsConnectorClient idsConnectorClient() {
        return new InMemoryIdsConnectorClient();
    }

    @Bean
    public BridgeLayer bridgeLayer(
            TdpPlatformClient tdpPlatformClient,
            IdsConnectorClient idsConnectorClient,
            ProtocolConverter protocolConverter,
            MessageAdapter messageAdapter,
            Participant overseasParticipant
    ) {
        BridgeLayer bridge = new BridgeLayer(tdpPlatformClient, idsConnectorClient, protocolConverter, messageAdapter);
        bridge.register(overseasParticipant);
        return bridge;
    }

    @Bean
    public ComplianceGateway complianceGateway(BridgeLayer bridgeLayer, GatewayProperties properties) {
        ComplianceGateway gateway = new ComplianceGateway(bridgeLayer);
        gateway.registerHook(new SensitiveDataHook());
        gateway.registerHook(new DestinationAllowlistHook(new LinkedHashSet<>(properties.getDestinationAllowlist())));
        return gateway;
    }

    @Bean
    public ContractSchemaValidator contractSchemaValidator() {
        return new ContractSchemaValidator();
    }

    @Bean
    public InteropPipeline interopPipeline(
            MessageAdapter messageAdapter,
            ProtocolConverter protocolConverter,
            BridgeLayer bridgeLayer,
            ComplianceGateway complianceGateway,
            MappingRegistry mappingRegistry,
            ContractSchemaValidator contractSchemaValidator
    ) {
        return new InteropPipeline(
                messageAdapter,
                protocolConverter,
                bridgeLayer,
                complianceGateway,
                mappingRegistry,
                contractSchemaValidator
        );
    }

    private static Participant toParticipant(GatewayProperties.ParticipantProperties props) {
        return new Participant(
                props.getTdpEntityId(),
                props.getTdpConnectorId(),
                props.getTdpConnectorName(),
                props.getIdsParticipantId(),
                props.getIdsConnectorUrl(),
                props.getRegion(),
                props.getDataspace()
        );
    }
}
