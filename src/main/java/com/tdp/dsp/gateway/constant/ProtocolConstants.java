package com.tdp.dsp.gateway.constant;

import java.util.Map;

/**
 * 国内可信数据空间（TDP）与国际数据空间协议（DSP）常量。
 */
public final class ProtocolConstants {

    public static final String DSP_CONTEXT = "https://w3id.org/dspace/2024/1/context.json";

    public static final String DSP_PATH_CATALOG_REQUEST = "/catalog/request";
    public static final String DSP_PATH_DATASET_REQUEST = "/catalog/datasets";
    public static final String DSP_PATH_NEGOTIATION_REQUEST = "/negotiations/request";
    public static final String DSP_PATH_TRANSFER_REQUEST = "/transfers/request";

    public static final String TDP_PATH_CATALOG_QUERY = "/catalogQuery";
    public static final String TDP_PATH_PRODUCT_DETAIL = "/productDetail";
    public static final String TDP_PATH_CONTRACT_CREATE = "/contractCreate";
    public static final String TDP_PATH_CONTRACT_NEGOTIATE = "/contractNegotiate";
    public static final String TDP_PATH_CONTRACT_REGISTRATE = "/contractRegistrate";
    public static final String TDP_PATH_CONTRACT_EXECUTION = "/contractExecution";
    public static final String TDP_PATH_CONTRACT_TERMINATE = "/contractTerminate";

    public static final String SIGN_MODE_P2P = "01";
    public static final String SIGN_MODE_PLATFORM = "02";

    public static final Map<String, String> TDP_ACTION_TO_ODRL = Map.ofEntries(
            Map.entry("读取", "odrl:read"),
            Map.entry("read", "odrl:read"),
            Map.entry("授权使用", "odrl:use"),
            Map.entry("use", "odrl:use"),
            Map.entry("转换", "odrl:derive"),
            Map.entry("transform", "odrl:derive"),
            Map.entry("derive", "odrl:derive"),
            Map.entry("匿名化", "odrl:anonymize"),
            Map.entry("anonymize", "odrl:anonymize"),
            Map.entry("脱敏", "tdp:desensitize"),
            Map.entry("desensitize", "tdp:desensitize"),
            Map.entry("出售", "odrl:sell"),
            Map.entry("sell", "odrl:sell"),
            Map.entry("追踪", "odrl:grantUse"),
            Map.entry("track", "odrl:grantUse")
    );

    public static final Map<String, String> ODRL_ACTION_TO_TDP = Map.of(
            "odrl:read", "读取",
            "odrl:use", "授权使用",
            "odrl:derive", "转换",
            "odrl:anonymize", "匿名化",
            "tdp:desensitize", "脱敏",
            "odrl:sell", "出售",
            "odrl:grantUse", "追踪"
    );

    public static final Map<String, String> TDP_OPERATOR_TO_ODRL = Map.ofEntries(
            Map.entry("01", "odrl:eq"),
            Map.entry("02", "odrl:gt"),
            Map.entry("03", "odrl:gteq"),
            Map.entry("04", "odrl:isPartOf"),
            Map.entry("05", "odrl:isA"),
            Map.entry("06", "odrl:isAllOf"),
            Map.entry("07", "odrl:isAnyOf"),
            Map.entry("08", "odrl:neq"),
            Map.entry("09", "odrl:isPartOf"),
            Map.entry("10", "odrl:lt"),
            Map.entry("11", "odrl:lteq"),
            Map.entry("12", "odrl:neq")
    );

    public static final Map<String, String> ODRL_OPERATOR_TO_TDP = Map.of(
            "odrl:eq", "01",
            "odrl:gt", "02",
            "odrl:gteq", "03",
            "odrl:isPartOf", "04",
            "odrl:isA", "05",
            "odrl:isAllOf", "06",
            "odrl:isAnyOf", "07",
            "odrl:neq", "12",
            "odrl:lt", "10",
            "odrl:lteq", "11"
    );

    public static final Map<String, String> TDP_CONSTRAINT_TO_ODRL = Map.of(
            "空间范围", "odrl:spatial",
            "spatial", "odrl:spatial",
            "时间范围", "odrl:dateTime",
            "datetime", "odrl:dateTime",
            "使用次数", "odrl:count",
            "count", "odrl:count",
            "使用目的", "odrl:purpose",
            "purpose", "odrl:purpose",
            "接收方", "odrl:recipient",
            "recipient", "odrl:recipient"
    );

    public static final Map<String, String> ODRL_CONSTRAINT_TO_TDP = Map.of(
            "odrl:spatial", "空间范围",
            "odrl:dateTime", "时间范围",
            "odrl:elapsedTime", "时间范围",
            "odrl:count", "使用次数",
            "odrl:purpose", "使用目的",
            "odrl:recipient", "接收方"
    );

    public static final String OP_CATALOG_QUERY = "catalogQuery";
    public static final String OP_PRODUCT_DETAIL = "productDetail";
    public static final String OP_CONTRACT_CREATE = "contractCreate";
    public static final String OP_CONTRACT_NEGOTIATE = "contractNegotiate";
    public static final String OP_CONTRACT_EXECUTION = "contractExecution";
    public static final String OP_CONTRACT_TERMINATE = "contractTerminate";

    public static final String DSP_CATALOG_REQUEST = "CatalogRequestMessage";
    public static final String DSP_DATASET_REQUEST = "DatasetRequestMessage";
    public static final String DSP_CONTRACT_REQUEST = "ContractRequestMessage";
    public static final String DSP_CONTRACT_AGREEMENT = "ContractAgreementMessage";
    public static final String DSP_CONTRACT_TERMINATION = "ContractNegotiationTerminationMessage";
    public static final String DSP_TRANSFER_REQUEST = "TransferRequestMessage";
    public static final String DSP_TRANSFER_START = "TransferStartMessage";
    public static final String DSP_CATALOG = "Catalog";
    public static final String DSP_DATASET = "Dataset";
    public static final String DSP_CONTRACT_NEGOTIATION = "ContractNegotiation";
    public static final String DSP_TRANSFER_PROCESS = "TransferProcess";

    private ProtocolConstants() {
    }
}
