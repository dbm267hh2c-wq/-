# 国内可信数据空间 ↔ 国际数据空间（DSP）互联互通网关

Java 21 Maven 工程。跨境网关按四层拆分，字段分三类落地：

| 类别 | 是否事先定义 | 落地点 | 改什么 |
| --- | --- | --- | --- |
| 协议标识（路径、`@type`、操作名） | 是 | `TdpOperation` / `DspMessageType` 枚举 | 改 Java |
| 字段名契约、语义码表 | 是，但是配置 | `src/main/resources/mappings/`、`schema/` | 改 JSON，不必改 Java |
| 产品名、合约 ID、约束值等实例 | 否 | 运行时报文 | 不预定义 |

新增「联合建模」这类操作行为：在 `mappings/semantic-codes.json` 加一条，或启动时用 overlay / 环境变量 `TDP_DSP_MAPPING_DIR` 覆盖，适配层会自动转换。

## 工程结构

```
src/main/java/com/tdp/dsp/gateway/
  protocol/          协议枚举（常量层）
  mapping/           码表加载、字段路径、Schema 校验
  layer/adapter      消息适配（读配置做转换）
  layer/protocol     协议转换（操作 ↔ DSP 消息）
  layer/bridge       境内平台 ↔ IDS 连接器
  layer/compliance   合规关口与扩展点
  http/              统一 HTTP 出入口
src/main/resources/
  mappings/semantic-codes.json     动作 / 运算符 / 约束码表
  mappings/field-mappings.json     字段路径契约 + 报文模板
  schema/*.schema.json             国内接口 JSON Schema
```

## 运行

```bash
mvn test
mvn -DskipTests package
java -jar target/tdp-dsp-gateway-1.0.0.jar 8080
```

可选覆盖目录：

```bash
export TDP_DSP_MAPPING_DIR=/etc/tdp-dsp/mappings
# 目录内放 semantic-overlay.json 即可追加码表
```

查看已加载码表：`GET /mappings`

## HTTP 出入口

入境：`POST /dsp/catalog/request`、`/dsp/negotiations/request`、`/dsp/transfers/request`

出境：`POST /tdp/catalogQuery`、`/productDetail`、`/contractCreate`、`/contractNegotiate`、`/contractExecution`、`/contractTerminate`

管理：`GET /health`、`/audit`、`/bridge/participants`、`/compliance/extensions`、`/mappings`

出境不符合 Schema 时返回 HTTP 400（`SCHEMA_INVALID`）。重要数据出境由合规关口返回 HTTP 403。
