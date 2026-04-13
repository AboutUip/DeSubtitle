# 运行时与边界

## 1. 形态

- **前端**：`web/index.html` 单页；逻辑在 `web/scripts/`，样式在 `web/styles/`，图标在 `web/icons/`，第三方库在 `web/plugins/`。
- **后端**：Spring Boot 进程内 **自行提供 `web/` 静态资源服务**（与 API 同源、同端口），实现类为 `WebDirectoryResourceConfig`；**不做**服务端模板渲染。REST 建议统一前缀 **`/api/**`**，以免与前端路由冲突。静态资源与 SPA 回退、状态码及 `/api` 规划的**总说明**见 [../api/backend-http-api.md](../api/backend-http-api.md)；**各 HTTP 端点专文**见 [../api/README.md](../api/README.md)。
- **配置**：`config/json/` 存可读写 JSON；`config/lua/` 存只读、带注释的 Lua 参考；合并与生效由 Java 实现。
- **数据**：持久化与运行期文件落在 `data/`，由 **Java 应用**负责创建、读写与生命周期（细则后续补充）。

## 2. 调用方向

```
浏览器  →  向同一 Spring 服务请求 `web/` 下 HTML/JS/CSS 等静态资源（根路径 `/` 重定向至 `/index.html`；无扩展名路径回退 `index.html`）
       →  调用 Spring Boot API（建议 `/api/**`；跨域仅在为独立前端源时配置 CORS）
       →  Java 读 config/lua（参考）、读/写 config/json（可改项）
       →  （可选）Java 调用阿里云视觉智能等外部服务
       →  Java 读/写 data/（数据库、文档等）
```

前端 **不** 直连阿里云 AccessKey；密钥与签名仅在后端。

## 3. 版本控制策略

| 区域 | Git |
|------|-----|
| `web/`（除构建产物若将来引入） | 提交 |
| `config/lua/*.lua`、`config/json/*.json`（默认与模板） | 提交 |
| `src/`、`docs/`、`pom.xml` 等 | 提交 |
| `data/` 下内容 | **以 `.gitignore` 为准**（常见为库文件与运行期文档不入库） |

克隆后若仓库中无 `data/` 目录，须由 **Java 在启动时创建** 或开发者本地建目录，再写入 SQLite 等文件。

## 4. 文档维护

- 架构与目录约定变更：改 `docs/architectuure/`。
- 阿里云接口与异步任务细节：改 `docs/reference/` 或官方链接，避免两处重复矛盾。
