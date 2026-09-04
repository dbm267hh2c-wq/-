package com.tdp.dsp.gateway.constant;

/**
 * 仅保留协议里稳定、可穷举、写错会直接不通的标识。
 * 字段名契约见 classpath:mappings/field-mappings.json，语义码表见 mappings/semantic-codes.json。
 */
public final class ProtocolConstants {

    public static final String DSP_CONTEXT = "https://w3id.org/dspace/2024/1/context.json";
    public static final String SIGN_MODE_P2P = "01";
    public static final String SIGN_MODE_PLATFORM = "02";

    private ProtocolConstants() {
    }
}
