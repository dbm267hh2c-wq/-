#!/usr/bin/env bash
set -euo pipefail

HOST="${LOGSTASH_HOST:-127.0.0.1}"
PORT="${LOGSTASH_HTTP_PORT:-8081}"
payload=${1:-'{"app":"tdp-dsp-gateway","level":"info","message":"logstash docker smoke test"}'}

echo "POST http://${HOST}:${PORT}/"
curl -sS -D - -o /tmp/logstash-http-body.txt \
  -H "Content-Type: application/json" \
  -X POST "http://${HOST}:${PORT}/" \
  -d "${payload}"
echo
echo "body:"
cat /tmp/logstash-http-body.txt
echo
