# Filebeat → Logstash 对接方案

适用场景：对方业务机安装 Filebeat，采集本地日志，通过 **TCP 5044** 推送到我方 Logstash。我方 Logstash 清洗后写入 Kafka。

本文只覆盖 **日志采集**。主机、应用端口、数据库、应用间调用链路见 [对方环境监控与调用链路对接方案.md](./对方环境监控与调用链路对接方案.md)。

本文可直接转发给对方运维。日志对接时对方机器 **只装 Filebeat**，不要再装 Logstash / Elasticsearch / Kibana。

---

## 1. 对接拓扑

```
对方业务服务器                     我方采集端
┌─────────────────────┐           ┌──────────────────────────────┐
│  应用日志文件        │  TCP 5044 │  Logstash                    │
│  Filebeat ───► beats├──────────►│  input.beats :5044           │
│                     │           │  filter 提取 host.name → name │
│ （可选）syslog/UDP   │  UDP 514  │  input.udp   :514            │
└─────────────────────┘           │  output.kafka                │
                                  └──────────────────────────────┘
```

| 项目 | 值 |
| --- | --- |
| 协议 | Beats / Lumberjack，**TCP** |
| 我方地址 | `172.28.65.248:5044` |
| 加密 | 当前未开 SSL，不要配证书 |
| Filebeat 版本 | **8.x**（与 Logstash 8 主版本对齐，建议 `8.19.19`） |
| 对方标识字段 | Filebeat 的 `name` → 事件里的 `host.name` → 我方最终字段 `name` |
| Kafka Topic（Beats） | `beats_data` |
| Kafka Topic（UDP 备选） | `udp_data` |

---

## 2. 网络与权限（先打通再装）

对方需要保证：

1. 业务机可以访问 `172.28.65.248:5044/tcp`（安全组、防火墙、专线都要放行）。
2. Filebeat 进程对日志文件有 **读权限**（常见坑：日志属主是应用用户，filebeat 用户读不到）。
3. 不要把 5044 配成 UDP，也不要写成 HTTP。
4. 一台机器只跑一个 Filebeat 对接我方，避免双实例抢同一批文件。

连通性自测（在对方服务器执行）：

```bash
# 能通才会出现 Connected
bash -c 'echo >/dev/tcp/172.28.65.248/5044' && echo OK || echo FAIL

# 或
nc -vz 172.28.65.248 5044
telnet 172.28.65.248 5044
```

不通时先找网络，不要反复重启 Filebeat。

---

## 3. 安装 Filebeat（Linux）

版本请与我方 Logstash 保持 **8.x**，不要装 7.x / 9.x。

### 3.1 CentOS / RHEL / 麒麟

```bash
rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch

cat > /etc/yum.repos.d/elastic-8.x.repo << 'EOF'
[elastic-8.x]
name=Elastic repository for 8.x packages
baseurl=https://artifacts.elastic.co/packages/8.x/yum
gpgcheck=1
gpgkey=https://artifacts.elastic.co/GPG-KEY-elasticsearch
enabled=1
autorefresh=1
type=rpm-md
EOF

yum install -y filebeat-8.19.19
```

无外网时：从 Elastic 官网下载对应架构的 rpm，拷到对方机器后执行：

```bash
rpm -ivh filebeat-8.19.19-x86_64.rpm
```

### 3.2 Ubuntu / Debian

```bash
wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | gpg --dearmor -o /usr/share/keyrings/elastic-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/elastic-keyring.gpg] https://artifacts.elastic.co/packages/8.x/apt stable main" \
  > /etc/apt/sources.list.d/elastic-8.x.list
apt-get update
apt-get install -y filebeat=8.19.19
```

### 3.3 安装后立刻关掉 ES 模块

```bash
filebeat modules disable system nginx mysql 2>/dev/null || true
systemctl stop filebeat
```

---

## 4. 对方必须改的配置

配置文件路径：`/etc/filebeat/filebeat.yml`

先备份再覆盖：

```bash
cp /etc/filebeat/filebeat.yml /etc/filebeat/filebeat.yml.bak.$(date +%F)
```

把第 11 节的完整配置拷进去后，**只改下面 3 处**：

| 配置项 | 怎么填 | 为什么 |
| --- | --- | --- |
| `filebeat.inputs.*.paths` | 对方真实日志路径，例如 `/app/logs/*.log` | 采错路径等于没对接 |
| `name` | 双方约定的机器标识，建议填 **业务 IP** | 我方 Logstash 用 `[host][name]` 生成 `name`，这是区分来源的关键 |
| `output.logstash.hosts` | 保持 `["172.28.65.248:5044"]` | 指向我方 Beats 端口 |

其余不要改：不要打开 `output.elasticsearch`，不要配 Kibana，不要开 SSL。

### 4.1 多行日志（必须和日志格式一致）

当前规则：

- 行首匹配 `YYYY-MM-DD`（如 `2026-09-18 12:00:01 ...`）视为一条新日志
- 后续堆栈、续行会合并到上一条

如果对方日志不是这个格式，必须改 `parsers.multiline.pattern`，否则堆栈会被拆碎，或所有行被粘成一条。

常见对照：

| 日志行首 | pattern |
| --- | --- |
| `2026-09-18 12:00:01` | `'^\d{4}-\d{2}-\d{2}'` |
| `2026/09/18` | `'^\d{4}/\d{2}/\d{2}'` |
| `[2026-09-18 12:00:01]` | `'^\[\d{4}-\d{2}-\d{2}'` |
| 单行日志、无堆栈 | 删掉整个 `parsers` 段 |

### 4.2 `id` 不要随便改

`filestream` 的 `id` 用来记采集进度（registry）。

同一台机器、同一批文件，**id 固定不变**。改了 id 会从头再采一遍，造成 Kafka 重复数据。

---

## 5. 启动与自检

```bash
# 语法检查，必须 exit 0
filebeat test config -c /etc/filebeat/filebeat.yml

# 测到我方 Logstash 的连接
filebeat test output -c /etc/filebeat/filebeat.yml

# 开机自启
systemctl enable --now filebeat
systemctl status filebeat --no-pager
journalctl -u filebeat -f
```

`test output` 成功时应看到类似 `write to ... 172.28.65.248:5044` / `connection established`。

看本机 Filebeat 是否在读文件：

```bash
ls -l /var/lib/filebeat/registry
tail -n 50 /var/log/filebeat/filebeat
```

日志里出现 `Harvester started for file` / `Active harvester` 表示已经开始采。

出现 `permission denied` 表示读不到日志文件，需要给 filebeat 用户加读权限，例如：

```bash
# 按实际日志目录调整
setfacl -m u:filebeat:rx /data/monitor/server/logs

# 或把 filebeat 加入应用日志组
usermod -aG <日志所属组> filebeat
systemctl restart filebeat
```

---

## 6. 我方 Logstash 侧会怎么处理

我方 Beats 管道会：

1. 给事件打上 `type = beats`
2. 取 `[host][name]` 写成 `name`
3. 删掉 `host` / `agent` / `ecs` / `tags` / `fields` / `@timestamp` / `input` / `log` / `event` 等元数据
4. JSON 写入 Kafka topic `beats_data`

因此对方最终进入 Kafka 的有效内容，主要是 **日志正文（`message`）+ `name`**。

`name` 填错、填重复、或留着系统随机主机名，后端将无法区分来源。

不要依赖 `fields:`、`tags:` 传业务标识——我方会删掉这些字段。标识只能放在 **`name` / `host.name`**。

---

## 7. 配置检查清单（对接前逐项打勾）

- [ ] Filebeat 版本 8.x，与我方 Logstash 主版本一致
- [ ] 只启用 `output.logstash`，ES / Kibana 全部关闭
- [ ] `hosts` 为 `172.28.65.248:5044`，协议 TCP
- [ ] `name` 已改成双方约定的唯一标识（建议 IP）
- [ ] `paths` 指向真实、正在滚动写入的日志
- [ ] 多行规则与日志行首格式一致
- [ ] `filestream.id` 固定，后续不改
- [ ] 未开启 SSL
- [ ] `filebeat test config`、`filebeat test output` 均成功
- [ ] 防火墙已放行到我方 5044
- [ ] filebeat 用户能读日志文件
- [ ] 未同时开 UDP syslog 和 Filebeat 采集同一份日志（会重复）

---

## 8. 常见故障

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `connection refused` / test output 失败 | 5044 未放行，或填成了对方自己的 IP | 核对我方地址，抓包 `tcpdump -nn port 5044` |
| `i/o timeout` | 中间防火墙丢包、安全组只开了 ICMP | 明确放行 TCP 5044 |
| 启动后无数据 | 路径不对、日志无新增、被 `ignore_older` 过滤 | 确认文件有新行；可临时去掉 `ignore_older` |
| `permission denied` | 读不到日志 | ACL 或把 filebeat 加入日志组 |
| Kafka 里 `name` 是 `%{[host][name]}` 或空 | 没带上 `host.name` | 必须保留 `add_host_metadata` + `copy_fields` |
| Kafka 里 `name` 是随机主机名 | 没改 `name:` | 改成约定 IP 后重启 |
| 堆栈被拆成多条 / 多条粘成一条 | 多行 pattern 不匹配 | 按第 4.1 节改 |
| 历史日志突然全量重发 | 改了 `filestream.id` 或清了 registry | 不要改 id，不要删 `/var/lib/filebeat/registry` |
| 重复数据 | 两台 Filebeat 采同一目录，或 Filebeat + UDP 同时发 | 只保留一种采集方式 |

---

## 9. 不要做的事

1. 不要把输出改成 Elasticsearch。
2. 不要在对方服务器再装一套 Logstash 转一层。
3. 不要用 Filebeat 7 对 Logstash 8（协议字段会差一截）。
4. 不要开启 `filebeat.autodiscover` / Docker / K8s 元数据（我方会丢掉，还浪费带宽）。
5. 不要采集 `/var/log/messages` 全量系统日志，除非双方书面约定。
6. 不要把 `bulk_max_size` 开到特别大却不限速，避免把我方 5044 打满。

---

## 10. 可选：UDP / syslog 对接（不装 Filebeat 时）

仅当对方设备只能发 syslog、不能装 Filebeat 时使用：

- 目标：`172.28.65.248:514/udp`
- 我方按来源 IP 写入 `name`，Kafka topic 为 `udp_data`
- **同一份日志不要 Filebeat 和 UDP 各发一次**

---

## 11. 对方可直接使用的 `filebeat.yml`

将下面内容保存为 `/etc/filebeat/filebeat.yml`，然后只改 `paths`、`name` 两处（`hosts` 仅在地址变化时才改）。

```yaml
###################### Filebeat → Logstash 对接配置 ######################
# 发给对方服务器使用。请按「必改项」修改后启动。
# Filebeat 版本必须与 Logstash 主版本一致（当前按 8.x）。

# ============================== Filebeat inputs ===============================
filebeat.inputs:
- type: filestream
  id: app-logs                    # 同一台机器上每个 input 的 id 必须唯一，改路径后不要改 id，否则会重复采集
  enabled: true
  paths:
    - /data/monitor/server/logs/*.log   # 【必改】实际日志路径，支持 glob
  # 可选：忽略多久未更新的文件，避免一次性扫历史全量
  ignore_older: 72h
  parsers:
    - multiline:
        type: pattern
        pattern: '^\d{4}-\d{2}-\d{2}'   # 行首为 YYYY-MM-DD 视为新日志；必须用单引号
        negate: true
        match: after

# 关闭 ES 模块，本方案只对接 Logstash
filebeat.config.modules:
  path: ${path.config}/modules.d/*.yml
  reload.enabled: false

# ================================== General ===================================
# 【必改】本机标识。Logstash 会把它落到事件的 name 字段（来自 host.name）
# 建议填本机业务 IP 或双方约定的主机名，不要留空、不要重复
name: "请改成对方服务器IP或主机名"

# 关闭模板/仪表盘，输出不是 Elasticsearch
setup.template.enabled: false
setup.ilm.enabled: false
setup.dashboards.enabled: false
setup.kibana.enabled: false

# ------------------------------ Logstash Output -------------------------------
output.elasticsearch:
  enabled: false

output.logstash:
  enabled: true
  hosts: ["172.28.65.248:5044"]   # 【必改确认】Logstash Beats 端口，必须是 TCP 5044
  # 未启用 SSL 时不要打开下面三项
  # ssl.certificate_authorities: ["/etc/pki/root/ca.pem"]
  # ssl.certificate: "/etc/pki/client/cert.pem"
  # ssl.key: "/etc/pki/client/cert.key"
  bulk_max_size: 2048
  worker: 2
  compression_level: 3
  ttl: 30s
  pipelining: 2

# ================================= Processors =================================
processors:
  - add_host_metadata:
      when.not.contains.tags: forwarded
  # 用配置里的 name 覆盖系统主机名，保证 Logstash 取到的 [host][name] 是双方约定标识
  - copy_fields:
      fields:
        - from: agent.name
          to: host.name
      fail_on_error: false
      ignore_missing: true
  - drop_fields:
      fields: ["cloud", "docker", "kubernetes"]
      ignore_missing: true

# ================================== Logging ===================================
logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  permissions: 0640
```

---

## 12. 对接完成后请对方回传

1. 对方服务器 IP
2. Filebeat 版本（`filebeat version`）
3. 配置的 `name`
4. 日志路径
5. `filebeat test output` 截图或完整输出
