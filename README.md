# 国内可信数据空间 ↔ 国际数据空间（DSP）互联互通网关

Java 实现的跨境数据空间桥接网关，按四层拆分：

1. **协议转换层**：把国内可信数据空间内部协议（目录查询、数字合约、合约履行等，对齐 TC609 / NDI-TR-2025）转换成国际 [Dataspace Protocol (DSP)](https://github.com/International-Data-Spaces-Association/ids-specification) 的 Catalog / Contract Negotiation / Transfer Process。
2. **消息适配层**：国内接口字段与 DSP JSON-LD（DCAT / ODRL）之间的格式适配、字段映射、语义转换（操作行为、约束运算符、约束名称）。
3. **桥接层**：登记并路由境内接入连接器 / 服务平台与 IDS 参与方连接器，完成入境（IDS → TDP）和出境（TDP → IDS）。
4. **合规关口层**：跨境请求唯一出入口，统一审计；`ComplianceHook` 预留数据分类分级、目的国白名单等后续合规校验点位。

## 架构

```
跨境调用（唯一出入口）
        │
        ▼
④ 合规关口 ComplianceGateway
   审计 + ComplianceHook 扩展点
        │
        ▼
③ 桥接 BridgeLayer
   参与方身份映射 / 路由到 TDP 平台或 IDS 连接器
        │
        ▼
② 消息适配 MessageAdapter     ① 协议转换 ProtocolConverter
   字段/语义                     操作与消息类型
```

## 运行

```bash
mvn -q test
mvn -q -DskipTests package
java -jar target/tdp-dsp-gateway-1.0.0.jar 8080
```

健康检查：`GET /health`

## HTTP 出入口

入境（IDS 消费方 → 国内平台）

- `POST /dsp/catalog/request`
- `POST /dsp/catalog/datasets`
- `POST /dsp/negotiations/request`
- `POST /dsp/transfers/request`

出境（国内连接器 → IDS）

- `POST /tdp/catalogQuery`
- `POST /tdp/productDetail`
- `POST /tdp/contractCreate`
- `POST /tdp/contractNegotiate`
- `POST /tdp/contractExecution`
- `POST /tdp/contractTerminate`

管理

- `GET /audit` 跨境流量审计
- `GET /bridge/participants` 参与方映射
- `GET /compliance/extensions` 合规扩展点

出境可带请求头：

- `X-IDS-Participant` 目标 IDS 参与方
- `X-Destination-Country` 目的国（供白名单扩展点使用）

## 字段与语义映射（节选）

| 国内 TDP | DSP |
| --- | --- |
| 数据产品 `dataProduct` | `dcat:Dataset` |
| 数据产品标识 `dataProductId` | `@id` |
| 数据产品名称 `dataProductName` | `dct:title` |
| 合约策略 `strategy` | `odrl:Offer` / `odrl:hasPolicy` |
| 操作行为 读取/授权使用/匿名化/脱敏 | `odrl:read` / `odrl:use` / `odrl:anonymize` / `tdp:desensitize` |
| 约束运算符 `01`–`12` | `odrl:eq` / `gt` / `gteq` / … |
| `/catalogQuery` | `dspace:CatalogRequestMessage` |
| `/contractCreate` `/contractNegotiate` | `dspace:ContractRequestMessage` |
| `/contractExecution` | `dspace:TransferRequestMessage` |

仓库内带内存版国内服务平台与 IDS 连接器，便于本地联调；将 `TdpPlatformClient` / `IdsConnectorClient` 换成 HTTP 客户端即可对接真实系统。
