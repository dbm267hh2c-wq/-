# 国内可信数据空间 ↔ 国际数据空间（DSP）互联互通网关

**Spring Boot 3.3 + Java 17** Maven 工程。跨境网关按四层拆分，字段分三类落地：

| 类别 | 是否事先定义 | 落地点 | 改什么 |
| --- | --- | --- | --- |
| 协议标识（路径、`@type`、操作名） | 是 | `TdpOperation` / `DspMessageType` 枚举 | 改 Java |
| 字段名契约、语义码表 | 是，但是配置 | `src/main/resources/mappings/`、`schema/` | 改 JSON，不必改 Java |
| 产品名、合约 ID、约束值等实例 | 否 | 运行时报文 | 不预定义 |

Web 层使用 Spring MVC：入境 `/dsp/**`、出境 `/tdp/**`、管理接口由 `@RestController` 提供。四层核心（协议转换、消息适配、桥接、合规关口）以 Spring Bean 装配。

## 工程结构

```
src/main/java/com/tdp/dsp/gateway/
  GatewayApplication.java   Spring Boot 启动类
  config/                   application.yml 绑定与 Bean 装配
  web/                      Spring MVC 出入境与管理接口
  protocol/                 协议枚举
  mapping/                  码表加载、字段路径、Schema 校验
  layer/adapter             消息适配
  layer/protocol            协议转换
  layer/bridge              境内平台 ↔ IDS 连接器
  layer/compliance          合规关口
src/main/resources/
  application.yml
  mappings/semantic-codes.json
  mappings/field-mappings.json
  schema/*.schema.json
```

## 运行

需要 JDK 17。

```bash
mvn test
mvn -DskipTests package
java -jar target/tdp-dsp-gateway-1.0.0.jar
```

配置见 `application.yml`：`tdp.dsp.mapping-dir`、本地/境外参与方、目的国白名单。也可用环境变量 `TDP_DSP_MAPPING_DIR`。

查看已加载码表：`GET /mappings`

## HTTP 出入口

入境：`POST /dsp/catalog/request`、`/dsp/negotiations/request`、`/dsp/transfers/request`

出境：`POST /tdp/catalogQuery`、`/productDetail`、`/contractCreate`、`/contractNegotiate`、`/contractExecution`、`/contractTerminate`

管理：`GET /health`、`/audit`、`/bridge/participants`、`/compliance/extensions`、`/mappings`

出境不符合 Schema 时返回 HTTP 400（`SCHEMA_INVALID`）。重要数据出境由合规关口返回 HTTP 403。
