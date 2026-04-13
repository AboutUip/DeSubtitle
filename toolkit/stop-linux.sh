#!/usr/bin/env bash
# 停止由 run-linux.sh 启动的 DeSubtitle 进程
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/desubtitle.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "未找到 PID 文件: $PID_FILE（可能未通过 run-linux.sh 启动）"
  exit 0
fi

pid="$(cat "$PID_FILE" || true)"
if [[ -z "${pid// }" ]]; then
  rm -f "$PID_FILE"
  echo "PID 文件为空，已清理。"
  exit 0
fi

if kill -0 "$pid" 2>/dev/null; then
  echo "停止进程 $pid …"
  kill "$pid" 2>/dev/null || true
  for _ in {1..30}; do
    if ! kill -0 "$pid" 2>/dev/null; then
      break
    fi
    sleep 1
  done
  if kill -0 "$pid" 2>/dev/null; then
    echo "进程未退出，发送 SIGKILL …"
    kill -9 "$pid" 2>/dev/null || true
  fi
else
  echo "进程 $pid 已不存在。"
fi

rm -f "$PID_FILE"
echo "已停止。"
