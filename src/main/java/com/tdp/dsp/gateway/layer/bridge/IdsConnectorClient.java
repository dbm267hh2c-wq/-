package com.tdp.dsp.gateway.layer.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 国际数据空间连接器客户端。
 */
public interface IdsConnectorClient {

    ObjectNode catalogRequest(String connectorUrl, ObjectNode dspMessage);

    ObjectNode datasetRequest(String connectorUrl, ObjectNode dspMessage);

    ObjectNode negotiationRequest(String connectorUrl, ObjectNode dspMessage);

    ObjectNode transferRequest(String connectorUrl, ObjectNode dspMessage);

    ObjectNode terminate(String connectorUrl, ObjectNode dspMessage);
}
