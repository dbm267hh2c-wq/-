package com.tdp.dsp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tdp.dsp.gateway.config.GatewayProperties;

/**
 * 国内可信数据空间 ↔ 国际数据空间（DSP/IDS）互联互通网关。
 * Spring Boot 3 + Java 17。
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
