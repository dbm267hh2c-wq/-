package com.tdp.dsp.gateway.layer.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.model.common.Participant;

/**
 * 境内可信数据空间服务平台客户端。
 */
public interface TdpPlatformClient {

    ObjectNode catalogQuery(ObjectNode query);

    ObjectNode productDetail(ObjectNode query);

    ObjectNode contractCreate(ObjectNode request);

    ObjectNode contractNegotiate(ObjectNode request);

    ObjectNode contractExecution(ObjectNode request);

    ObjectNode contractTerminate(ObjectNode request);

    Participant localParticipant();
}
