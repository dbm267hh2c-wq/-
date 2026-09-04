package com.tdp.dsp.gateway.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tdp.dsp.gateway.json.Jsons;
import com.tdp.dsp.gateway.pipeline.InteropPipeline;
import com.tdp.dsp.gateway.protocol.DspMessageType;
import com.tdp.dsp.gateway.protocol.TdpOperation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * 跨境请求统一 HTTP 出入口。入境走 DSP HTTPS Binding 路径，出境走国标连接器路径。
 */
public class GatewayHttpServer {

    private final InteropPipeline pipeline;
    private final int port;
    private HttpServer server;

    public GatewayHttpServer(InteropPipeline pipeline, int port) {
        this.pipeline = pipeline;
        this.port = port;
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/health", this::health);
        server.createContext("/audit", this::audit);
        server.createContext("/bridge/participants", this::participants);
        server.createContext("/compliance/extensions", this::extensions);
        server.createContext("/mappings", this::mappings);
        server.createContext("/dsp/catalog/request", exchange -> inbound(exchange, DspMessageType.CATALOG_REQUEST.typeName()));
        server.createContext("/dsp/catalog/datasets", exchange -> inbound(exchange, DspMessageType.DATASET_REQUEST.typeName()));
        server.createContext("/dsp/negotiations/request", exchange -> inbound(exchange, DspMessageType.CONTRACT_REQUEST.typeName()));
        server.createContext("/dsp/transfers/request", exchange -> inbound(exchange, DspMessageType.TRANSFER_REQUEST.typeName()));
        server.createContext("/tdp/catalogQuery", exchange -> outbound(exchange, TdpOperation.CATALOG_QUERY));
        server.createContext("/tdp/productDetail", exchange -> outbound(exchange, TdpOperation.PRODUCT_DETAIL));
        server.createContext("/tdp/contractCreate", exchange -> outbound(exchange, TdpOperation.CONTRACT_CREATE));
        server.createContext("/tdp/contractNegotiate", exchange -> outbound(exchange, TdpOperation.CONTRACT_NEGOTIATE));
        server.createContext("/tdp/contractExecution", exchange -> outbound(exchange, TdpOperation.CONTRACT_EXECUTION));
        server.createContext("/tdp/contractTerminate", exchange -> outbound(exchange, TdpOperation.CONTRACT_TERMINATE));
        server.setExecutor(null);
        server.start();
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int port() {
        return server == null ? port : server.getAddress().getPort();
    }

    private void health(HttpExchange exchange) throws IOException {
        writeJson(exchange, 200, Jsons.objectOf(
                "status", "UP",
                "service", "tdp-dsp-gateway",
                "layers", List.of("protocol", "adapter", "bridge", "compliance")
        ));
    }

    private void audit(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Jsons.objectOf("message", "仅支持 GET"));
            return;
        }
        writeJson(exchange, 200, pipeline.complianceGateway().auditView());
    }

    private void participants(HttpExchange exchange) throws IOException {
        writeJson(exchange, 200, pipeline.bridgeLayer().directory());
    }

    private void extensions(HttpExchange exchange) throws IOException {
        writeJson(exchange, 200, Jsons.objectOf(
                "extensionPoints", pipeline.complianceGateway().extensionPoints(),
                "registeredHooks", pipeline.complianceGateway().hooks().stream().map(hook -> hook.name()).toList()
        ));
    }

    private void mappings(HttpExchange exchange) throws IOException {
        writeJson(exchange, 200, pipeline.mappingRegistry().snapshot());
    }

    private void inbound(HttpExchange exchange, String dspType) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Jsons.objectOf("message", "仅支持 POST"));
            return;
        }
        ObjectNode body = readBody(exchange);
        ObjectNode metadata = metadataFrom(exchange);
        metadata.put("peer", exchange.getRemoteAddress().toString());
        ObjectNode response = pipeline.inboundDsp(dspType, body, metadata);
        int status = "CatalogError".equals(Jsons.typeName(response)) ? 403 : 200;
        writeJson(exchange, status, response);
    }

    private void outbound(HttpExchange exchange, TdpOperation operation) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Jsons.objectOf("message", "仅支持 POST"));
            return;
        }
        ObjectNode body = readBody(exchange);
        java.util.List<String> schemaErrors = pipeline.schemaValidator().validate(operation.schemaFile(), body);
        if (!schemaErrors.isEmpty()) {
            writeJson(exchange, 400, Jsons.objectOf(
                    "status", "1",
                    "code", "SCHEMA_INVALID",
                    "message", "报文不符合字段契约",
                    "errors", schemaErrors
            ));
            return;
        }
        ObjectNode metadata = metadataFrom(exchange);
        metadata.put("peer", exchange.getRemoteAddress().toString());
        ObjectNode response = pipeline.outboundTdp(operation.code(), body, metadata);
        int status = "1".equals(Jsons.text(response, "status")) ? 403 : 200;
        writeJson(exchange, status, response);
    }

    private ObjectNode metadataFrom(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        ObjectNode metadata = Jsons.object();
        String ids = header(headers, "X-IDS-Participant");
        if (ids != null) {
            metadata.put("idsParticipantId", ids);
        }
        String country = header(headers, "X-Destination-Country");
        if (country != null) {
            metadata.put("destinationCountry", country);
        }
        return metadata;
    }

    private static String header(Headers headers, String name) {
        List<String> values = headers.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private ObjectNode readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            byte[] bytes = input.readAllBytes();
            if (bytes.length == 0) {
                return Jsons.object();
            }
            JsonNode node = Jsons.MAPPER.readTree(bytes);
            return Jsons.requireObject(node);
        }
    }

    private void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = Jsons.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
