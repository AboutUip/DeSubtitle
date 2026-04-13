# 文档编写约束

本文约束 **Agent 与本仓库协作者** 在 `docs/` 下编写、修改 Markdown 时的规则。硬性禁止项另见 [hard-constraints.md](./hard-constraints.md)。

## 1. 适用范围

- 凡路径在 `docs/` 下的 `.md` 文件均适用。
- 根目录 `README.md` 等若属「项目说明」，风格上对齐本文**结构清晰、少废话**原则；具体是否拆分由用户决定。

## 2. 风格与结构

- **分主题拆文件**，用各子目录的 `README.md` 做索引表（路径、一句话摘要），避免单文件过长。
- 优先使用**标题层级 + 列表 + 表格**传递信息；避免散文式铺陈、重复同义句。
- **禁止**：平台营销话术、无信息增量的「快照」式描述、冗长引言与收束套话。

## 3. 语言

- 面向本仓库读者的说明正文默认使用**简体中文**（专有名词、API 字段名、代码标识符保持原文）。

## 4. 目录分工

| 路径 | 内容类型 |
|------|-----------|
| `docs/architectuure/` | 仓库布局、运行时边界、前后端职责；**含 [java-modules.md](../architectuure/java-modules.md)（main 源码每文件一职）** |
| `docs/reference/` | 外部服务/API/SDK 参考、可核对官方链接 |
| `docs/api/` | **本仓库 Java 提供的 HTTP**：横切约定见 `backend-http-api.md`；**每个 Controller 端点一篇专文**（如 `life.md`），并在 [README.md](../api/README.md) 索引 |
| `docs/config/` | **每个**配置文件一篇 `lua-*.md` / `json-*.md`；**[README.md](../config/README.md)** 为索引表，与仓库内 `config` 文件同步 |
| `docs/sqldb/` | SQLite `desubtitle.db` 表与列；**[README.md](../sqldb/README.md)** 为索引 |
| `docs/restriction/` | 对 Agent 的限制与协作约定（含本文） |

新增主题时归入上表之一；若不确定，在任务中向用户确认后再落盘。

## 5. 更新策略

- **直接覆盖**：以当前事实为准改写原文，**不**写迁移指南、**不**双轨保留旧版章节、**不**为旧锚点/章节编号做向前兼容。
- 删除失效内容即可，**不**单独开「已废弃」长节（除非用户要求保留审计痕迹）。

## 6. 与「勿滥增文档」的关系

- **禁止**在用户未要求时新增与任务无关的 Markdown（见 [hard-constraints.md](./hard-constraints.md) §2）。
- 用户明确要求写文档时，**须**遵守本文 §2～§5。

## 7. 维护方式

- 用户或 Agent 对文档规则有新约定时：**直接修改本文**（或按主题拆出新文件并在本子目录 [README.md](./README.md) 或 [docs 根 README](../README.md) 索引中登记），不另写过渡文档。
