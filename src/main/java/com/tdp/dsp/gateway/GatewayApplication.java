package com.tdp.dsp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tdp.dsp.gateway.config.GatewayProperties;

/**
 * 国内可信数据空间 ↔ 国际数据空间（DSP / IDS）互联互通网关的 Spring Boot 入口。
 *
 * <p>本工程按四层拆分跨境流量：
 * <ol>
 *   <li>协议转换：国内操作枚举 ↔ DSP {@code @type} / 路径</li>
 *   <li>消息适配：按外置字段契约与语义码表做字段/码值转换</li>
 *   <li>桥接：境内服务平台客户端 ↔ IDS 连接器客户端，并维护参与方身份映射</li>
 *   <li>合规关口：唯一跨境出入口，先审计与合规钩子，再交给桥接层</li>
 * </ol>
 *
 * <p>运行时：Spring Boot 3.3 + Java 17。配置前缀 {@code tdp.dsp}，见 {@link GatewayProperties}。
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayApplication {

    /**
     * 启动内嵌 Tomcat，装配四层 Bean 与 MVC 出入境接口。
     *
     * @param args 标准 Spring Boot 启动参数，例如 {@code --server.port=8080}
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
