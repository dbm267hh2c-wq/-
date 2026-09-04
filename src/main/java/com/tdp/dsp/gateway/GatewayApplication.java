package com.tdp.dsp.gateway;

import com.tdp.dsp.gateway.http.GatewayHttpServer;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;

/**
 * 国内可信数据空间 ↔ 国际数据空间（DSP/IDS）互联互通网关。
 */
public final class GatewayApplication {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        InteropPipeline pipeline = InteropPipeline.createDefault();
        GatewayHttpServer server = new GatewayHttpServer(pipeline, port);
        server.start();
        System.out.println("TDP-DSP 互联互通网关已启动: http://127.0.0.1:" + port);
        System.out.println("四层：协议转换 / 消息适配 / 桥接 / 合规关口");
        System.out.println("健康检查: GET /health");
        System.out.println("入境 DSP 目录: POST /dsp/catalog/request");
        System.out.println("出境 TDP 目录: POST /tdp/catalogQuery");
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        Thread.currentThread().join();
    }
}
