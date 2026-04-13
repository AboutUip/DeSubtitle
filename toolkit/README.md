# DeSubtitle 工具脚本

本目录提供在 **Linux** 与 **Windows** 上编译并后台运行后端服务的脚本，以及对应的停止脚本。  
服务为 Spring Boot 可执行 JAR，静态前端由应用从仓库根目录的 `web/` 提供。默认 HTTP 端口见根目录 `config/lua/ports.lua`（一般为 **8080**）。

## 文件说明

| 文件 | 说明 |
|------|------|
| `run-linux.sh` | Linux：若无合适 JDK，则用 **apt** 安装 **`openjdk-25-jdk`**；执行 `mvnw` 打包；`nohup` 启动并写入 `desubtitle.pid` |
| `stop-linux.sh` | 根据 `desubtitle.pid` 结束进程并删除 PID 文件 |
| `run-windows.ps1` | Windows：使用本机已安装的 `java` 与 `mvnw.cmd` 打包并启动；日志在 `logs\` |
| `stop-windows.ps1` | 根据 `desubtitle.pid` 结束进程 |

运行产生的日志目录：`toolkit/logs/`（若不存在会自动创建）。**请勿将 `desubtitle.pid`、日志提交到版本库**（仓库根 `.gitignore` 已忽略 `toolkit/logs/` 与 `toolkit/desubtitle.pid`）。

## Linux

```bash
chmod +x toolkit/run-linux.sh toolkit/stop-linux.sh
./toolkit/run-linux.sh
```

停止：

```bash
./toolkit/stop-linux.sh
```

要求：系统提供 **apt-get**（Debian/Ubuntu 等）；安装 JDK 时会执行 **`sudo apt-get update`** 与 **`sudo apt-get install -y openjdk-25-jdk`**（已 root 运行则不加 sudo）。请确保官方或发行版软件源中已有 **OpenJDK 25**（较旧发行版可能没有该包，需升级系统或换源）。

可选环境变量：

- `JDK_FEATURE_VERSION`：默认 `25`，对应 apt 包名 `openjdk-${JDK_FEATURE_VERSION}-jdk`  
- `LOG_DIR`：应用标准输出/错误追加日志目录，默认 `toolkit/logs`  

若已手动安装同主版本 JDK，可预先导出 **`JAVA_HOME`** 指向其安装目录，脚本在检测到 `java -version` 主版本已匹配时会跳过 apt 安装。

## Windows

在 PowerShell 中于仓库根目录执行：

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned   # 若尚未允许本地脚本，仅需一次
.\toolkit\run-windows.ps1
```

停止：

```powershell
.\toolkit\stop-windows.ps1
```

要求：`java` 与 Maven Wrapper（`mvnw.cmd`）可用；JDK 主版本需不低于 `pom.xml` 中 `java.version`（当前为 **17**）。

## 说明

- 打包均使用 `-DskipTests` 以加快部署；需要跑测试时在仓库根目录自行执行 `./mvnw test` 或 `.\mvnw.cmd test`。  
- 若 apt 源中尚无 **openjdk-25-jdk**，请升级系统/换用较新 Ubuntu 或 Debian，或自行安装 JDK 25 并设置 **JAVA_HOME** 后再运行脚本（脚本将跳过 apt 安装）。  
- 生产环境请另行配置进程守护、反向代理与 `DESUBTITLE_JWT_SECRET` 等环境变量（参见项目 `docs/`）。
