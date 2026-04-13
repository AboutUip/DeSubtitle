# 停止由 run-windows.ps1 启动的 DeSubtitle 进程
#Requires -Version 5.1
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $ScriptDir "desubtitle.pid"

if (-not (Test-Path $PidFile)) {
    Write-Host "未找到 PID 文件: $PidFile（可能未通过 run-windows.ps1 启动）"
    exit 0
}

$raw = Get-Content -Raw $PidFile
if (-not $raw) {
    Remove-Item -Force $PidFile
    Write-Host "PID 文件为空，已清理。"
    exit 0
}

$id = [int]($raw.Trim())
try {
    Stop-Process -Id $id -Force -ErrorAction Stop
    Write-Host "已停止进程 $id"
}
catch {
    Write-Host "进程 $id 已不存在或无法停止: $_"
}

Remove-Item -Force $PidFile -ErrorAction SilentlyContinue
Write-Host "完成。"
