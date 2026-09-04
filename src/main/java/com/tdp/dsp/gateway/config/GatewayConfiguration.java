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

/**
 * 四层核心与支撑组件的 Spring 装配。
 *
 * <p>依赖顺序：码表 → 适配 → 协议转换 → 双边客户端 → 桥接（注册境外参与方）
 * → 合规关口（挂敏感数据 / 目的地白名单钩子）→ 流水线门面。
 *
 * <p>当前客户端是内存模拟，便于单机联调；对接真实平台时替换
 * {@link TdpPlatformClient} / {@link IdsConnectorClient} 的 Bean 即可，不必改四层编排。
 */
@Configuration
public class GatewayConfiguration {

    /** 启动加载 classpath 码表，并按 {@link GatewayProperties#getMappingDir()} 合并外部覆盖。 */
    @Bean
    public MappingRegistry mappingRegistry(GatewayProperties properties) {
        return MappingRegistry.fromClasspath(properties.getMappingDir());
    }

    /** ② 消息适配：叶子字段走码表，列表/嵌套策略仍由适配层组装。 */
    @Bean
    public MessageAdapter messageAdapter(MappingRegistry mappingRegistry) {
        return new MessageAdapter(mappingRegistry);
    }

    /** ① 协议转换：国内操作 ↔ DSP 消息类型，并委托适配层填业务字段。 */
    @Bean
    public ProtocolConverter protocolConverter(MessageAdapter messageAdapter) {
        return new ProtocolConverter(messageAdapter);
    }

    /** 本侧参与方，同时作为内存 TDP 平台里模拟产品的属主。 */
    @Bean
    public Participant localParticipant(GatewayProperties properties) {
        return toParticipant(properties.getLocal());
    }

    /** 境外 IDS 参与方，启动时写入桥接层目录，供出境路由解析。 */
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

    /**
     * ③ 桥接层。构造时会自动注册本侧参与方；这里再注册境外参与方，避免出境找不到对端。
     */
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

    /**
     * ④ 合规关口。钩子按注册顺序短路：任一 DENY 即拦截，不再调用下游桥接。
     * 白名单为空时 {@link DestinationAllowlistHook} 直接放行。
     */
    @Bean
    public ComplianceGateway complianceGateway(BridgeLayer bridgeLayer, GatewayProperties properties) {
        ComplianceGateway gateway = new ComplianceGateway(bridgeLayer);
        gateway.registerHook(new SensitiveDataHook());
        gateway.registerHook(new DestinationAllowlistHook(new LinkedHashSet<>(properties.getDestinationAllowlist())));
        return gateway;
    }

    /** 出境 Controller 在进流水线前做国内报文 Schema 校验。 */
    @Bean
    public ContractSchemaValidator contractSchemaValidator() {
        return new ContractSchemaValidator();
    }

    /**
     * HTTP / 单测共用的四层门面。跨境请求必须走 {@link InteropPipeline#inboundDsp} /
     * {@link InteropPipeline#outboundTdp}，不得绕过合规关口。
     */
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

    /** 把 yml 扁平字段转成桥接层使用的 {@link Participant} 记录。 */
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
