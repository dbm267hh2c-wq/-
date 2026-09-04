package com.tdp.dsp.gateway.protocol;

import com.tdp.dsp.gateway.json.Jsons;

import java.util.Arrays;

/**
 * Dataspace Protocol 消息类型。
 *
 * <p>{@code typeName} 对应 JSON-LD {@code @type} 的短名（去掉 {@code dspace:} 前缀后比较）。
 * {@code dspPath} 是规范路径，{@code gatewayPath} 是本网关入境前缀 {@code /dsp} 之后的映射。
 *
 * <p>目录 / 协商 / 传输的请求与过程对象都列在此，协议转换层用请求类型选国内操作，
 * 用过程对象（Catalog、ContractNegotiation 等）构造回包。
 */
public enum DspMessageType {
    CATALOG_REQUEST("CatalogRequestMessage", "/catalog/request", "/dsp/catalog/request"),
    DATASET_REQUEST("DatasetRequestMessage", "/catalog/datasets", "/dsp/catalog/datasets"),
    CONTRACT_REQUEST("ContractRequestMessage", "/negotiations/request", "/dsp/negotiations/request"),
    CONTRACT_AGREEMENT("ContractAgreementMessage", "/negotiations/agreement", "/dsp/negotiations/agreement"),
    CONTRACT_TERMINATION("ContractNegotiationTerminationMessage", "/negotiations/termination", "/dsp/negotiations/termination"),
    TRANSFER_REQUEST("TransferRequestMessage", "/transfers/request", "/dsp/transfers/request"),
    TRANSFER_START("TransferStartMessage", "/transfers/start", "/dsp/transfers/start"),
    CATALOG("Catalog", "/catalog", "/dsp/catalog"),
    DATASET("Dataset", "/catalog/datasets", "/dsp/catalog/datasets"),
    CONTRACT_NEGOTIATION("ContractNegotiation", "/negotiations", "/dsp/negotiations"),
    TRANSFER_PROCESS("TransferProcess", "/transfers", "/dsp/transfers");

    private final String typeName;
    private final String dspPath;
    private final String gatewayPath;

    DspMessageType(String typeName, String dspPath, String gatewayPath) {
        this.typeName = typeName;
        this.dspPath = dspPath;
        this.gatewayPath = gatewayPath;
    }

    /** JSON-LD {@code @type} 短名，入境流水线 {@code operation} 使用此值。 */
    public String typeName() {
        return typeName;
    }

    public String dspPath() {
        return dspPath;
    }

    public String gatewayPath() {
        return gatewayPath;
    }

    /**
     * 接受完整 IRI（{@code dspace:CatalogRequestMessage}）或短名 / 枚举名。
     */
    public static DspMessageType fromTypeName(String value) {
        String shortName = Jsons.shortName(value);
        return Arrays.stream(values())
                .filter(item -> item.typeName.equals(shortName) || item.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的 DSP 消息类型: " + value));
    }
}
