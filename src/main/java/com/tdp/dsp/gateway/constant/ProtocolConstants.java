package com.tdp.dsp.gateway.constant;

/**
 * 仅保留协议里稳定、可穷举、写错会直接不通的标识。
 *
 * <p>不要把字段名或业务码值堆在这里：
 * <ul>
 *   <li>字段路径契约 → {@code mappings/field-mappings.json}</li>
 *   <li>语义码表（动作 / 运算符 / 约束名） → {@code mappings/semantic-codes.json}</li>
 *   <li>产品名、合约 ID、约束取值 → 运行时报文</li>
 * </ul>
 */
public final class ProtocolConstants {

    /** DSP 2024-1 JSON-LD 上下文，所有出境 DSP 报文 {@code @context} 使用此值。 */
    public static final String DSP_CONTEXT = "https://w3id.org/dspace/2024/1/context.json";

    /** 国内合约签署模式：点对点签署。 */
    public static final String SIGN_MODE_P2P = "01";

    /**
     * 国内合约签署模式：平台代签。
     * 入境 DSP 合约请求转 {@code /contractCreate} 时默认用此模式，因为网关作为平台出入口。
     */
    public static final String SIGN_MODE_PLATFORM = "02";

    private ProtocolConstants() {
    }
}
