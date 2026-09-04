package com.tdp.dsp.gateway.layer.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tdp.dsp.gateway.model.common.Participant;

/**
 * 境内可信数据空间服务平台客户端。
 *
 * <p>方法名与国标操作一一对应。当前默认实现是内存模拟；对接真实平台时替换此接口的 Bean，
 * 桥接层调用点无需改动。
 */
public interface TdpPlatformClient {

    ObjectNode catalogQuery(ObjectNode query);

    ObjectNode productDetail(ObjectNode query);

    ObjectNode contractCreate(ObjectNode request);

    ObjectNode contractNegotiate(ObjectNode request);

    ObjectNode contractExecution(ObjectNode request);

    ObjectNode contractTerminate(ObjectNode request);

    /** 本侧参与方，用于填入境请求的 issuer 与目录属主。 */
    Participant localParticipant();
}
