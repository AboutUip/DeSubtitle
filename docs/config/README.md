# 配置项文档索引（`docs/config/`）

本目录为 **`config/lua/*.lua`**、**`config/json/*.json`** 与 **`config/yml/*.yml`** 维护**一配置文件一篇**说明（作用、影响、支持的值、默认值）。Lua / JSON / YML 职责边界见 [../architectuure/repository-layout.md](../architectuure/repository-layout.md) 中 `@config` 一节。

## 命名规则

| 配置文件 | 文档文件名 |
|----------|------------|
| `config/lua/<name>.lua` | `lua-<name>.md` |
| `config/json/<name>.json` | `json-<name>.md` |
| `config/yml/<name>.yml` | `yml-<name>.md` |

新增或重命名仓库内配置文件时，**须**按上表新增/重命名对应文档，并更新下表。

## 文档一览

| 配置文件 | 说明文档 | 摘要 |
|----------|----------|------|
| `config/yml/application.yml` | [yml-application.md](./yml-application.md) | Spring Boot 主配置（数据源、JPA、multipart、`desubtitle.*` 等）；由 classpath `application.properties` 仅通过 `spring.config.import` 加载 |
| `config/lua/ports.lua` | [lua-ports.md](./lua-ports.md) | 启动前注入 `server.port`、`desubtitle.frontend.port` |
| `config/lua/runtime_mode.lua` | [lua-runtime_mode.md](./lua-runtime_mode.md) | 运行模式只读参考（如 `debug_mode`）；读取逻辑待接线 |
| `config/lua/user_token.lua` | [lua-user_token.md](./lua-user_token.md) | 匿名临时 JWT 有效期（`token_ttl_minutes`），`GET /getUserToken` |
| `config/lua/video_upload.lua` | [lua-video_upload.md](./lua-video_upload.md) | 每用户视频上传上限（`max_videos_per_user`），`POST /uploadVideo` |
| `config/json/runtime.json` | [json-runtime.md](./json-runtime.md) | 可写运行态（如 `initialization_completed`）；显式读写后生效 |
| `config/json/agreement.json` | [json-agreement.md](./json-agreement.md) | 用户协议正文（`text`），由 `GET /getAgreement` 输出 |

返回总文档目录：[docs/README.md](../README.md)。
