<p align="center">
  <img src="web/icons/logo.svg" alt="DeSubtitle" width="128" height="128" />
</p>

<h1 align="center">DeSubtitle</h1>

<p align="center">
  量子像素场景下的 Web 去字幕服务：基于 <strong>阿里云视频增强 EraseVideoSubtitles</strong>，由 Spring Boot 提供 API 与静态站点托管，SQLite 持久化，浏览器单页应用操作上传与任务进度。
</p>

---

## 功能概览

- **视频上传与配额**：多路并行处理路数由后端配置下发，上传落盘与过期清理策略可由 `config` 调整。
- **去字幕流水线**：调用阿里云异步擦除字幕，轮询结果并下载成品至本地 `data/desubtitle/`。
- **用户与鉴权**：匿名用户 JWT（Bearer），初始化向导配置阿里云凭证（非调试环境写入约定存储；密钥禁止进仓库配置）。
- **单页前端**：仓库根目录 `web/` 由 Java 进程映射，无需单独静态服务器即可访问（跨域开发场景见 `config/yml/application.yml` 注释）。
- **用户协议文案**：`GET /getAgreement` 读取 `config/json/agreement.json` 中的 `text`，用于前端展示。

更细的 HTTP 端点、字段与行为见 [docs/api/README.md](docs/api/README.md)。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java **17**（`pom.xml`）、Spring Boot **4**、Spring Data JPA、SQLite（Hibernate 社区方言） |
| 集成 | 阿里云 videoenhan（擦字幕）、credentials-java；JWT（jjwt） |
| 配置 | `config/lua/*.lua`（启动前只读，含端口等）、`config/json/*.json`（可读写运行期状态） |
| 前端 | 原生 HTML/CSS/JS（`web/scripts/`、`web/styles/`） |
| 构建 | Maven Wrapper（`mvnw` / `mvnw.cmd`） |

## 仓库结构（摘要）

| 路径 | 说明 |
|------|------|
| `src/main/java` | Spring Boot 应用、REST、领域服务、启动与配置加载 |
| `web/` | 单页入口 `index.html`、脚本、样式、**图标含 [logo.svg](web/icons/logo.svg)** |
| `config/` | `lua/` 默认与说明；`json/` 运行时可改参数 |
| `data/` | SQLite 与上传/成品视频目录（默认被 `.gitignore` 忽略，部署时由服务创建） |
| `docs/` | **完整文档索引** [docs/README.md](docs/README.md)：架构、API 专文、配置项、约束与外部参考 |
| `toolkit/` | Linux（apt 安装 OpenJDK 25 + 打包 + nohup）与 Windows（打包 + 后台启动）脚本及 [toolkit/README.md](toolkit/README.md) |

完整目录约定见 [docs/architectuure/repository-layout.md](docs/architectuure/repository-layout.md)。

## 环境要求

- **JDK**：编译与运行需满足 `pom.xml` 中 `java.version`（当前为 **17**）。使用 `toolkit/run-linux.sh` 时脚本会尝试通过 **apt** 安装 **OpenJDK 25**（与编译目标 17 兼容）；Windows 脚本依赖本机已安装的 JDK。
- **网络**：构建拉取 Maven 依赖；运行期访问阿里云 OpenAPI 与异步任务结果 URL。
- **环境变量**：生产环境请配置 **`DESUBTITLE_JWT_SECRET`**（长度需满足 HS256 要求）；阿里云 **AccessKey** 通过初始化接口或文档约定方式提供，**不要**写入 `config/json` 或 `config/lua`。详见 [docs/restriction/hard-constraints.md](docs/restriction/hard-constraints.md) 与测试/部署文档。

## 快速开始

### 本地开发（仓库根目录）

```bash
# 设置 JWT 密钥（示例，生产请换为安全随机串）
export DESUBTITLE_JWT_SECRET=0123456789abcdef0123456789abcdef   # Linux / macOS
# Windows PowerShell: $env:DESUBTITLE_JWT_SECRET="..."

./mvnw -DskipTests package
java -jar target/DeSubtitle-0.0.1-SNAPSHOT.jar
```

Windows 将 `./mvnw` 换为 `.\mvnw.cmd`。默认 HTTP 端口由 [config/lua/ports.lua](config/lua/ports.lua) 在启动时注入（一般为 **8080**）。浏览器访问 `http://localhost:8080/`（以实际端口为准）。

### 脚本化部署

- **Linux**：`chmod +x toolkit/run-linux.sh toolkit/stop-linux.sh` 后执行 `./toolkit/run-linux.sh`（apt + sudo 安装 `openjdk-25-jdk` 等），停止用 `./toolkit/stop-linux.sh`。
- **Windows**：PowerShell 执行 `.\toolkit\run-windows.ps1`，停止用 `.\toolkit\stop-windows.ps1`。

说明与约束见 [toolkit/README.md](toolkit/README.md)。

## 配置与文档入口

- **端口与前端端口**：`config/lua/ports.lua`
- **上传大小、并行路数、擦除区域等**：`config/yml/application.yml` 与对应 `config/lua` 文档（索引：[docs/config/README.md](docs/config/README.md)）
- **用户可见协议正文**：`config/json/agreement.json`（字段 `text`），说明见 [docs/config/json-agreement.md](docs/config/json-agreement.md)
- **数据库表结构**：[docs/sqldb/schema.md](docs/sqldb/schema.md)

## 测试

```bash
./mvnw test
```

Surefire 已为测试环境注入最小长度 JWT 密钥；具体见 `pom.xml` 中 `maven-surefire-plugin` 配置。

## 协作与规范

新增 API、配置键或跨模块行为时，请同步更新 `docs/` 下对应专文（约定见 [docs/restriction/documentation-conventions.md](docs/restriction/documentation-conventions.md)）。

---

**文档总索引**：[docs/README.md](docs/README.md)
