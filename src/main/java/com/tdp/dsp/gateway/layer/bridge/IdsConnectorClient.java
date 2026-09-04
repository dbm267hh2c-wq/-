package com.tdp.dsp.gateway.layer.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 国际数据空间连接器客户端。
 *
 * <p>方法对应 DSP Catalog / Negotiation / Transfer。{@code connectorUrl} 是对端基址，
 * 由桥接层从参与方目录解析后传入。
 */
public interface IdsConnectorClient {

    ObjectNode catalogRequest(String connectorUrl, ObjectNode dspMessage);

    ObjectNode datasetRequest(String connectorUrl, ObjectNode dspMessage);

    ObjectNode negotiationRequest(String connectorUrl, ObjectNode dspMessage);

    ObjectNode transferRequest(String connectorUrl, ObjectNode dspMessage);

    ObjectNode terminate(String connectorUrl, ObjectNode dspMessage);
}
