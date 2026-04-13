# 仓库与目录布局

以下路径均相对于**仓库根目录**。命名与拼写以本文为准：`plugins` 为第三方 JS 库目录（勿使用错误拼写 `pulgins`）。

## 总览

| 路径 | 代号 | 职责 |
|------|------|------|
| `web/` | @web | 浏览器端静态资源与**唯一** HTML 入口 |
| `src/` | @src | Java 源码（Spring Boot，仅提供 API） |
| `config/` | @config | 双目录：`json/` 可读写（运行时可改）；`lua/` 只读参考（注释友好，不写回） |
| `data/` | @data | 运行时数据区：SQLite 库文件、由 Java 生成或维护的文档/缓存等 |
| `docs/` | @docs | 项目文档（非运行时依赖） |

## `web/`（@web）

单页应用（SPA）：**全站仅一个 HTML 文件**，其余为脚本、样式与资源引用。

| 路径 | 职责 |
|------|------|
| `web/index.html` | 唯一页面入口；通过 `script` / `link` 挂载 `scripts`、`styles`、`plugins` |
| `web/icons/` | 站点图标（SVG）：`logo.svg`（`favicon` / 顶栏 / 启动）、`spinner.svg`、`arrow-right.svg`、`folder.svg` 等 |
| `web/plugins/` | 第三方 JS 库（如打包后的单文件、vendor 脚本），**不放业务逻辑** |
| `web/scripts/` | 项目自有 **JavaScript** 全部集中于此（可按功能拆多文件，由入口统一加载或构建；当前含 `app.js` 入口、`api.js` 请求封装、`ui-shell.js` Toast/确认框） |
| `web/styles/` | 项目自有 **样式表**（CSS 等）全部集中于此 |

约定：

- 不在 `web/` 下再引入第二套「页面级」HTML（多页模式禁止）。
- 业务请求一律指向后端 API，由 `web/scripts/` 内代码发起（`fetch` / `XMLHttpRequest` 等）。
- 开发/部署时由 **Java 后端从仓库根目录 `web/` 映射静态资源**（工作目录须能解析到该目录）；无需单独再启一个静态文件服务器即可访问页面。

## `src/`（@src）

- Java 包结构与 Spring Boot 应用主类、配置、领域服务、REST Controller 等。
- **不**承担主要 UI 渲染；前端以 `web/` 为准。
- 每个 `src/main/java` 下源文件的**唯一职责**见 [java-modules.md](./java-modules.md)（须随代码维护；**不含**测试目录说明）。

## `config/`（@config）

| 子目录 | 格式 | 职责 |
|--------|------|------|
| `config/json/` | `.json` | **可改**：应用可读写，承载运行期可调参数与持久化需求（可多文件按域拆分）。仓库内约定主文件为 `runtime.json`（如初始化完成标记等）。 |
| `config/lua/` | `.lua` | **仅可读**：面向人的说明与默认值参考，支持注释；**不**作为应用写回目标。其中 `ports.lua` 在 Spring 启动前由 `StartupLuaPorts` 读取，注入 `server.port` 与 `desubtitle.frontend.port`。 |
| `config/yml/` | `.yml` | **只读部署侧**：Spring Boot 主配置（如 `application.yml`）。由 `src/main/resources/application.properties` 中 **`spring.config.import=file:./config/yml/application.yml`** 从工作目录加载；路径相对 JVM `user.dir`。 |

- **Lua 配置项与注释（强制）**：`return { ... }` 中**每一个**配置键（含嵌套表内作为独立配置含义的键）**必须**附有**专属于该键**的注释：优先写在该键**紧邻的上一行** `-- …`，或键**同一行行尾** `-- …`；禁止仅用文件头笼统描述代替逐项注释。见 [../restriction/code-conventions.md](../restriction/code-conventions.md) §4。
- 合并策略（先读 Lua 再叠 JSON、或反之）在业务代码中实现；Spring 层键值以 **`config/yml/application.yml`** 为主，classpath 仅保留 `spring.config.import` 引导项。
- **禁止**在 `json`/`lua`/`yml` 中存放密钥；AccessKey 等按 [../restriction/hard-constraints.md](../restriction/hard-constraints.md) §1 使用环境变量。
- 新增键名时同步文档或模块注释；**每个** `*.lua` / `*.json` / `config/yml/*.yml` 在 `docs/config/` 下有独立说明文档，并在 [../config/README.md](../config/README.md) 索引表中登记（命名 `lua-<文件名>.md` / `json-<文件名>.md` / `yml-<文件名>.md`）。

## `data/`（@data）

- 存放 **SQLite `.db` 文件**（路径由 `config/yml/application.yml` 中 `spring.datasource.url` 等配置，且文件位于 `data/` **根层**，不建子目录存放库文件）。
- **`data/videos/`**：`POST /uploadVideo` 落盘的源视频；**`data/desubtitle/`**：`POST /sendToDeSubtitle` 从阿里云结果 URL 下载的去字幕后视频（随机文件名，保留期见 `video_upload.lua`）。
- 其它 **由 Java 在运行时写入或维护的文档类产物** 仍归本目录，具体规则随功能补充。
- **是否提交 Git**：以仓库根 `.gitignore` 与用户约定为准；运行前若需 `data/` 目录，由 Java 启动逻辑或本地手动创建。

## `docs/`（@docs）

| 路径 | 职责 |
|------|------|
| `docs/architectuure/` | 本目录：系统架构与仓库/边界级约定 |
| `docs/reference/` | 参考目录：外部 API、SDK、参数与运维要点 |
| `docs/restriction/` | 对 Agent 的限制与告知（非业务功能说明） |

## 与构建/运行的关系

- 开发时：前端可由静态服务器或浏览器直接打开 `web/index.html`（需注意跨域时后端需配置 CORS）；后端独立进程提供 API；`config/lua/*.lua` 与 `config/json/*.json` 的加载与合并时机以实现为准。
- 生产部署时：可将 `web/` 挂到 CDN/Nginx，或将静态资源与 Spring Boot 的静态资源目录对齐（实现阶段再定，不改变上述**源码仓库**布局约定）。
