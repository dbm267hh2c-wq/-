# 在 Docker 容器里安装 Logstash

用 Ubuntu 24.04 作为基础镜像，**在构建容器时下载并安装** Elastic 官方 Logstash 8.17 发行包（自带 JDK），不依赖 Elasticsearch。

## 前置条件

- Docker Engine 20.10+
- Docker Compose v2（`docker compose version`）
- 建议可用内存 ≥ 2 GB

## 安装步骤（推荐：Dockerfile 构建）

在仓库根目录执行：

```bash
cd docker/logstash
cp .env.example .env
```

`.env` 里可改版本和堆内存：

```bash
LOGSTASH_VERSION=8.17.0
LS_JAVA_OPTS=-Xms512m -Xmx512m
```

构建镜像（这一步会在容器内安装 Logstash）：

```bash
docker compose build
```

启动容器：

```bash
docker compose up -d
docker compose ps
```

首次启动大约 30–90 秒。`STATUS` 变为 `healthy` 后验证：

```bash
curl -s http://127.0.0.1:9600
docker run --rm tdp-dsp-logstash:8.17.0 logstash -V
```

发送测试事件：

```bash
./scripts/send-test-event.sh
```

看到 HTTP `200` 且 `output/events-YYYY.MM.dd.ndjson` 有内容，即安装成功。

构建 + 启动也可合并成：

```bash
docker compose up -d --build
```

## 容器里实际做了什么

`Dockerfile` 相当于在容器内执行：

1. 基于 `ubuntu:24.04` 安装 `curl`、`ca-certificates`、`tini`
2. 创建 `logstash` 用户（UID 1000）
3. 从 Elastic 官方地址下载 `logstash-8.17.0-linux-x86_64.tar.gz`
4. 解压到 `/usr/share/logstash`
5. 写入本仓库的 `pipeline/logstash.conf` 和 `config/logstash.yml`
6. 以 `logstash` 用户启动 `logstash`

对应命令（构建时自动执行，不必手敲）：

```bash
curl -fsSL https://artifacts.elastic.co/downloads/logstash/logstash-8.17.0-linux-x86_64.tar.gz \
  -o /tmp/logstash.tar.gz
tar -xzf /tmp/logstash.tar.gz -C /usr/share
mv /usr/share/logstash-8.17.0 /usr/share/logstash
```

## 方法二：进入空白容器手动安装

适合先摸索、再固化到 Dockerfile。

```bash
docker run -it --name ls-manual --hostname logstash \
  -p 5044:5044 -p 8081:8081 -p 50000:50000 -p 9600:9600 \
  ubuntu:24.04 bash
```

容器内：

```bash
apt-get update
apt-get install -y --no-install-recommends ca-certificates curl
curl -fsSL https://artifacts.elastic.co/downloads/logstash/logstash-8.17.0-linux-x86_64.tar.gz \
  -o /tmp/logstash.tar.gz
tar -xzf /tmp/logstash.tar.gz -C /usr/share
mv /usr/share/logstash-8.17.0 /usr/share/logstash
export PATH=/usr/share/logstash/bin:$PATH
logstash --version
```

把本仓库的 `pipeline/`、`config/logstash.yml` 拷进容器后再启动：

```bash
# 在宿主机执行
docker cp pipeline/. ls-manual:/usr/share/logstash/pipeline/
docker cp config/logstash.yml ls-manual:/usr/share/logstash/config/logstash.yml
docker exec -it ls-manual bash -lc 'logstash'
```

手动安装只用于试验；日常请用上面的 `docker compose build`。

## 端口

| 端口 | 用途 |
| --- | --- |
| `5044` | Filebeat / Elastic Agent（Beats 协议） |
| `8081` | HTTP JSON 接入 |
| `50000` | TCP JSON Lines |
| `9600` | Logstash 监控 API |

网关占用 `8080`，所以 HTTP 接入用 `8081`。

## 接入日志

1. HTTP：`POST http://<主机>:8081/`，Content-Type 为 `application/json`
2. 文件：把 `.log` / `.json` / `.ndjson` 放到 `logs-in/`
3. Filebeat：输出到 `logstash:5044`

处理后的事件：

- 容器标准输出：`docker compose logs -f logstash`
- 文件：`output/events-YYYY.MM.dd.ndjson`

## 停止与卸载

```bash
docker compose stop          # 停止
docker compose down          # 删除容器，保留数据卷
docker compose down -v       # 同时删除 logstash-data 卷
docker rmi tdp-dsp-logstash:8.17.0   # 删除本仓库构建的镜像
```

## 常见问题

- **构建时下载失败**：检查能否访问 `artifacts.elastic.co`。可把安装包提前下到构建机，再改 Dockerfile 用 `COPY`。
- **一直 unhealthy**：冷启动较慢，看日志是否出现 `Successfully started Logstash API endpoint`。
- **权限错误**：容器内 `logstash` 用户 UID 为 1000，保证宿主机 `output/` 可写。
- **改了 pipeline 不生效**：已开启 `config.reload.automatic`；未生效则 `docker compose restart logstash`。
- **内存不足**：`.env` 里把 `LS_JAVA_OPTS` 调到 `-Xms1g -Xmx1g`。
