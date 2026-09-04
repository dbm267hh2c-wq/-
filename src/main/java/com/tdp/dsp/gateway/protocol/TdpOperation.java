package com.tdp.dsp.gateway.protocol;

import java.util.Arrays;
import java.util.Locale;

/**
 * 国内可信数据空间操作。标识来自国标接口，用枚举固定，不放业务字段值。
 */
public enum TdpOperation {
    CATALOG_QUERY("catalogQuery", "/catalogQuery", "/tdp/catalogQuery", "tdp-catalog-query.schema.json"),
    PRODUCT_DETAIL("productDetail", "/productDetail", "/tdp/productDetail", "tdp-product-detail.schema.json"),
    CONTRACT_CREATE("contractCreate", "/contractCreate", "/tdp/contractCreate", "tdp-contract-create.schema.json"),
    CONTRACT_NEGOTIATE("contractNegotiate", "/contractNegotiate", "/tdp/contractNegotiate", null),
    CONTRACT_EXECUTION("contractExecution", "/contractExecution", "/tdp/contractExecution", null),
    CONTRACT_TERMINATE("contractTerminate", "/contractTerminate", "/tdp/contractTerminate", null);

    private final String code;
    private final String domesticPath;
    private final String gatewayPath;
    private final String schemaFile;

    TdpOperation(String code, String domesticPath, String gatewayPath, String schemaFile) {
        this.code = code;
        this.domesticPath = domesticPath;
        this.gatewayPath = gatewayPath;
        this.schemaFile = schemaFile;
    }

    public String code() {
        return code;
    }

    public String domesticPath() {
        return domesticPath;
    }

    public String gatewayPath() {
        return gatewayPath;
    }

    public String schemaFile() {
        return schemaFile;
    }

    public static TdpOperation fromCode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("国内协议操作为空");
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(item -> item.code.equals(normalized)
                        || item.domesticPath.equals(normalized)
                        || item.gatewayPath.equals(normalized)
                        || item.name().equals(normalized.toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的国内协议操作: " + value));
    }
}
