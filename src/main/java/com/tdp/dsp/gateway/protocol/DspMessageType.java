package com.tdp.dsp.gateway.protocol;

import com.tdp.dsp.gateway.json.Jsons;

import java.util.Arrays;

/**
 * DSP 消息类型。标识来自 Dataspace Protocol，用枚举固定。
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

    public String typeName() {
        return typeName;
    }

    public String dspPath() {
        return dspPath;
    }

    public String gatewayPath() {
        return gatewayPath;
    }

    public static DspMessageType fromTypeName(String value) {
        String shortName = Jsons.shortName(value);
        return Arrays.stream(values())
                .filter(item -> item.typeName.equals(shortName) || item.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的 DSP 消息类型: " + value));
    }
}
