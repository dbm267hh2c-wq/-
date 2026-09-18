# 在 Ubuntu 容器内安装 Logstash（含自带 JDK，不依赖 Elasticsearch）
FROM ubuntu:24.04

ARG LOGSTASH_VERSION=8.17.0
ARG TARGETARCH=amd64
ENV DEBIAN_FRONTEND=noninteractive \
    LOGSTASH_VERSION=${LOGSTASH_VERSION} \
    LS_HOME=/usr/share/logstash \
    PATH=/usr/share/logstash/bin:$PATH

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        tini \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    groupadd --system --gid 1000 logstash; \
    useradd --system --uid 1000 --gid 1000 --home-dir /usr/share/logstash --shell /usr/sbin/nologin logstash; \
    case "${TARGETARCH}" in \
      amd64) arch=x86_64 ;; \
      arm64) arch=aarch64 ;; \
      *) echo "unsupported TARGETARCH=${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    curl -fsSL "https://artifacts.elastic.co/downloads/logstash/logstash-${LOGSTASH_VERSION}-linux-${arch}.tar.gz" \
      -o /tmp/logstash.tar.gz; \
    tar -xzf /tmp/logstash.tar.gz -C /usr/share; \
    mv "/usr/share/logstash-${LOGSTASH_VERSION}" /usr/share/logstash; \
    rm -f /tmp/logstash.tar.gz /usr/share/logstash/pipeline/logstash.conf; \
    mkdir -p /usr/share/logstash/data /usr/share/logstash/logs /usr/share/logstash/output /usr/share/logstash/logs-in; \
    chown -R logstash:logstash /usr/share/logstash

COPY --chown=logstash:logstash pipeline/ /usr/share/logstash/pipeline/
COPY --chown=logstash:logstash config/logstash.yml /usr/share/logstash/config/logstash.yml

USER logstash
WORKDIR /usr/share/logstash
EXPOSE 5044 8081 50000 9600
ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["logstash"]
