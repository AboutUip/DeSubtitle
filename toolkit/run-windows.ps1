# DeSubtitle：Windows 下编译并启动服务（需本机已安装 JDK，版本需满足 pom 中 java.version）
#Requires -Version 5.1
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Root = Split-Path -Parent $ScriptDir
$PidFile = Join-Path $ScriptDir "desubtitle.pid"
$LogDir = Join-Path $ScriptDir "logs"
$Jar = Join-Path $Root "target\DeSubtitle-0.0.1-SNAPSHOT.jar"
$Mvnw = Join-Path $Root "mvnw.cmd"

if (Test-Path $PidFile) {
    $old = (Get-Content -Raw $PidFile).Trim()
    if ($old) {
        $p = Get-Process -Id $old -ErrorAction SilentlyContinue
        if ($p) {
            Write-Error "服务已在运行 (PID $old)。请先执行 toolkit\stop-windows.ps1"
        }
        Remove-Item -Force $PidFile
    }
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "未找到 java，请先安装 JDK 并加入 PATH。"
}

Set-Location $Root
Write-Host "Maven 打包（跳过测试）…"
& $Mvnw -q "-DskipTests" package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not (Test-Path $Jar)) {
    Write-Error "未找到 JAR: $Jar"
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$outLog = Join-Path $LogDir "app.log"
$errLog = Join-Path $LogDir "app.err.log"

$proc = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", $Jar) `
    -WorkingDirectory $Root `
    -WindowStyle Hidden `
    -PassThru `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog

Set-Content -Path $PidFile -Value $proc.Id -NoNewline
Write-Host "已启动 DeSubtitle，PID $($proc.Id)"
Write-Host "日志: $outLog 与 $errLog"
Write-Host "默认 HTTP 端口见 config\lua\ports.lua（一般为 8080）。"
