# Logstash Docker 安装

使用 Elastic 官方镜像在 Docker 中运行 Logstash 8.17，**不依赖 Elasticsearch**。适合先验证采集链路，再按需接入 ES / Kafka。

镜像：`docker.elastic.co/logstash/logstash:8.17.0`  
配置目录：`docker/logstash/`

## 前置条件

- Docker Engine 20.10+
- Docker Compose v2（`docker compose version`）
- 建议可用内存 ≥ 2 GB（堆默认 512 MB）

## 快速安装

```bash
cd docker/logstash
cp .env.example .env   # 可选，按需改镜像或堆内存
docker compose pull
docker compose up -d
docker compose ps
```

首次启动大约 30–90 秒。健康检查通过后：

```bash
curl -s http://127.0.0.1:9600 | python3 -m json.tool
```

看到 `"status": "green"` 或 `"status": "yellow"` 即表示 API 已就绪。

## 端口

| 端口 | 用途 |
| --- | --- |
| `5044` | Filebeat / Elastic Agent（Beats 协议） |
| `8081` | HTTP JSON 接入（测试最方便） |
| `50000` | TCP JSON Lines |
| `9600` | Logstash 监控 API |

网关本身占用 `8080`，因此 HTTP 接入使用 `8081`。

## 发送测试事件

```bash
chmod +x scripts/send-test-event.sh
./scripts/send-test-event.sh
```

或手动：

```bash
curl -H 'Content-Type: application/json' \
  -X POST http://127.0.0.1:8081/ \
  -d '{"app":"tdp-dsp-gateway","level":"info","message":"hello logstash"}'
```

处理后的事件会：

1. 打印到容器标准输出：`docker compose logs -f logstash`
2. 写入 `output/events-YYYY.MM.dd.ndjson`

把日志文件放到 `logs-in/` 也会被 `file` 输入读取。

## 仅用 docker run

```bash
docker run -d --name tdp-dsp-logstash \
  -e LS_JAVA_OPTS="-Xms512m -Xmx512m" \
  -p 5044:5044 -p 8081:8081 -p 50000:50000 -p 9600:9600 \
  -v "$PWD/pipeline:/usr/share/logstash/pipeline:ro" \
  -v "$PWD/config/logstash.yml:/usr/share/logstash/config/logstash.yml:ro" \
  -v "$PWD/logs-in:/usr/share/logstash/logs-in:ro" \
  -v "$PWD/output:/usr/share/logstash/output" \
  docker.elastic.co/logstash/logstash:8.17.0
```

必须挂载自己的 `pipeline/`，否则会沿用镜像自带的 Beats → stdout 示例配置。

不要把整个 `config/` 目录挂进去（会覆盖镜像内的 `log4j2.properties`、`jvm.options`）。只挂 `logstash.yml`。

## 构建自定义镜像

配置打进镜像、生产环境不想 bind-mount 时：

```bash
docker build -t tdp-dsp-logstash:8.17.0 .
docker run -d --name tdp-dsp-logstash \
  -e LS_JAVA_OPTS="-Xms512m -Xmx512m" \
  -p 5044:5044 -p 8081:8081 -p 50000:50000 -p 9600:9600 \
  tdp-dsp-logstash:8.17.0
```

## 国内拉镜像失败

`docker.elastic.co` 超时或被拦截时，改 `.env`：

```bash
LOGSTASH_IMAGE=elastic/logstash:8.17.0
```

或配置 Docker 镜像加速后重试 `docker compose pull`。

## 接入本仓库网关日志

1. **HTTP**：应用把 JSON 日志 POST 到 `http://<logstash-host>:8081/`。
2. **文件**：将网关日志目录挂到 `logs-in/`，或在 `docker-compose.yml` 再加一条 volume。
3. **Filebeat**：采集主机日志后输出到 `logstash:5044`。

接入 Elasticsearch 时，打开 `pipeline/logstash.conf` 底部注释掉的 `elasticsearch` 输出，并自行增加 ES 服务（需要额外内存与 `vm.max_map_count`）。

## 停止与卸载

```bash
docker compose stop          # 停止
docker compose down          # 删除容器，保留数据卷
docker compose down -v       # 同时删除 logstash-data 卷
```

## 常见问题

- **一直 unhealthy**：Logstash 冷启动较慢，看 `docker compose logs logstash` 是否已出现 `Successfully started Logstash API endpoint`。
- **权限错误**：容器内 `logstash` 用户 UID 为 1000，请保证主机上 `output/` 可写。
- **改了 pipeline 不生效**：已开启 `config.reload.automatic`。若 inotify 未触发，执行 `docker compose restart logstash`。
- **内存不足**：把 `.env` 里 `LS_JAVA_OPTS` 调到 `-Xms1g -Xmx1g`，或关闭其它占用内存的容器。
