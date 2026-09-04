package com.tdp.dsp.gateway.protocol;

import java.util.Arrays;
import java.util.Locale;

/**
 * 国内可信数据空间操作标识。
 *
 * <p>路径、操作码、对应 Schema 文件名属于协议合同，写错会直接对不上国标接口，因此用枚举固定。
 * 产品名、合约 ID 等业务实例值不出现在这里。
 *
 * <p>{@link #fromCode(String)} 同时接受操作码、国内路径、网关路径、枚举名，方便 HTTP 与配置混用。
 */
public enum TdpOperation {
    CATALOG_QUERY("catalogQuery", "/catalogQuery", "/tdp/catalogQuery", "tdp-catalog-query.schema.json"),
    PRODUCT_DETAIL("productDetail", "/productDetail", "/tdp/productDetail", "tdp-product-detail.schema.json"),
    CONTRACT_CREATE("contractCreate", "/contractCreate", "/tdp/contractCreate", "tdp-contract-create.schema.json"),
    CONTRACT_NEGOTIATE("contractNegotiate", "/contractNegotiate", "/tdp/contractNegotiate", null),
    CONTRACT_EXECUTION("contractExecution", "/contractExecution", "/tdp/contractExecution", null),
    CONTRACT_TERMINATE("contractTerminate", "/contractTerminate", "/tdp/contractTerminate", null);

    /** 国标操作码，出境流水线 {@code operation} 使用此值。 */
    private final String code;

    /** 国内平台原始路径（不含网关前缀）。 */
    private final String domesticPath;

    /** 本网关对外暴露的出境路径。 */
    private final String gatewayPath;

    /**
     * classpath {@code schema/} 下的校验文件；{@code null} 表示该操作暂不做入站 Schema 校验。
     */
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

    /**
     * 按操作码 / 路径 / 枚举名解析。大小写不敏感仅针对枚举名。
     *
     * @throws IllegalArgumentException 无法识别时抛出，避免静默落到错误操作
     */
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
