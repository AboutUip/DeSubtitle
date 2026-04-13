# 文档索引（`docs/`）

本目录收录 **DeSubtitle** 的架构、API、配置项、外部参考与协作约束。各主题**分目录**维护；进入任一目录请先阅读其 **`README.md`**，再按需打开正文链接。

| 子目录 | 索引 | 概要 |
|--------|------|------|
| [architectuure/](./architectuure/README.md) | [architectuure/README.md](./architectuure/README.md) | 仓库布局、运行时边界、Java 主源码模块一览 |
| [api/](./api/README.md) | [api/README.md](./api/README.md) | 静态与 SPA 总约定；**各端点独立文档**（如 `GET /life` → `life.md`） |
| [config/](./config/README.md) | [config/README.md](./config/README.md) | `config/lua`、`config/json`、`config/yml` 各文件的独立配置说明（索引 + 命名规则） |
| [reference/](./reference/README.md) | [reference/README.md](./reference/README.md) | 外部服务 / SDK 等第三方参考 |
| [sqldb/](./sqldb/README.md) | [sqldb/README.md](./sqldb/README.md) | `data/desubtitle.db` 表结构与列说明 |
| [restriction/](./restriction/README.md) | [restriction/README.md](./restriction/README.md) | Agent 与协作者的硬性约束、代码与文档规范 |

编写或修改 `docs/` 下内容时，遵守 [restriction/documentation-conventions.md](./restriction/documentation-conventions.md)。
