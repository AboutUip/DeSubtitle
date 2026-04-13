#!/usr/bin/env bash
# DeSubtitle：Linux 一键环境（apt 安装 JDK 25）+ 编译 + nohup 后台启动
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT"

JDK_FEATURE_VERSION="${JDK_FEATURE_VERSION:-25}"
LOG_DIR="${LOG_DIR:-"$SCRIPT_DIR/logs"}"
PID_FILE="$SCRIPT_DIR/desubtitle.pid"
JAR="$ROOT/target/DeSubtitle-0.0.1-SNAPSHOT.jar"
APT_PKG="openjdk-${JDK_FEATURE_VERSION}-jdk"

need_cmd() { command -v "$1" >/dev/null 2>&1; }

java_major() {
  if need_cmd java; then
    java -version 2>&1 | head -n1 | sed -E 's/.* version "([0-9]+).*/\1/' || true
  else
    echo ""
  fi
}

discover_openjdk_home() {
  local d
  for d in "/usr/lib/jvm/java-${JDK_FEATURE_VERSION}-openjdk-"*; do
    if [[ -x "$d/bin/java" ]]; then
      echo "$d"
      return 0
    fi
  done
  return 1
}

install_jdk_apt() {
  if ! need_cmd apt-get; then
    echo "未找到 apt-get。本脚本仅支持使用 apt 的发行版（如 Debian/Ubuntu）；请自行安装 JDK ${JDK_FEATURE_VERSION} 后再运行。" >&2
    exit 1
  fi

  local sudo_cmd=()
  if [[ "${EUID:-0}" -ne 0 ]]; then
    if ! need_cmd sudo; then
      echo "安装 JDK 需要 root 权限，但未找到 sudo。请以 root 运行本脚本。" >&2
      exit 1
    fi
    sudo_cmd=(sudo)
  fi

  echo "通过 apt 安装 ${APT_PKG} …"
  DEBIAN_FRONTEND=noninteractive "${sudo_cmd[@]}" apt-get update
  DEBIAN_FRONTEND=noninteractive "${sudo_cmd[@]}" apt-get install -y "$APT_PKG"

  local home
  home="$(discover_openjdk_home || true)"
  if [[ -z "$home" ]]; then
    echo "已安装 ${APT_PKG}，但未在 /usr/lib/jvm/ 下找到 java-${JDK_FEATURE_VERSION}-openjdk-*。请检查软件源或手动设置 JAVA_HOME。" >&2
    exit 1
  fi
  export JAVA_HOME="$home"
  export PATH="$JAVA_HOME/bin:$PATH"
}

ensure_jdk25() {
  local maj
  maj="$(java_major)"
  if [[ "$maj" == "$JDK_FEATURE_VERSION" ]]; then
    echo "已检测到 Java ${JDK_FEATURE_VERSION}，跳过安装。"
    if [[ -z "${JAVA_HOME:-}" ]] && home="$(discover_openjdk_home 2>/dev/null || true)" && [[ -n "$home" ]]; then
      export JAVA_HOME="$home"
      export PATH="$JAVA_HOME/bin:$PATH"
    fi
    return 0
  fi

  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
    maj="$(java_major)"
    if [[ "$maj" == "$JDK_FEATURE_VERSION" ]]; then
      echo "使用已配置的 JAVA_HOME: $JAVA_HOME"
      return 0
    fi
  fi

  install_jdk_apt
  maj="$(java_major)"
  if [[ "$maj" != "$JDK_FEATURE_VERSION" ]]; then
    echo "apt 安装后 java 主版本仍非 ${JDK_FEATURE_VERSION}，请确认软件源提供 ${APT_PKG}。" >&2
    exit 1
  fi
  echo "JAVA_HOME=$JAVA_HOME"
}

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "服务已在运行 (PID $(cat "$PID_FILE"))。先执行: $SCRIPT_DIR/stop-linux.sh" >&2
  exit 1
fi

ensure_jdk25

chmod +x "$ROOT/mvnw" 2>/dev/null || true
echo "Maven 打包（跳过测试）…"
"$ROOT/mvnw" -q -DskipTests package

mkdir -p "$LOG_DIR"
nohup java -jar "$JAR" >>"$LOG_DIR/app.log" 2>&1 &
echo $! >"$PID_FILE"
echo "已启动 DeSubtitle，PID $(cat "$PID_FILE")，日志: $LOG_DIR/app.log"
echo "默认 HTTP 端口见 config/lua/ports.lua（一般为 8080）。"
