# 系统架构文档（索引）

本目录描述 **DeSubtitle** 的仓库结构、前后端边界与静态资源约定。本仓库 **Java 提供的 HTTP 与 REST 说明**见 [../api/README.md](../api/README.md)。**配置项**见 [../config/README.md](../config/README.md)（索引各 `lua-*.md` / `json-*.md` 与 `config/` 下文件的对应关系）。对外部厂商接口的字段级说明见 [../reference/README.md](../reference/README.md)。对 Agent 的行为约束见 [../restriction/README.md](../restriction/README.md)。

| 文档 | 内容 |
|------|------|
| [仓库与目录布局](./repository-layout.md) | `web`、`src`、`data`、`docs` 等路径职责 |
| [运行时与边界](./runtime-boundaries.md) | 单页前端、Spring Boot API、数据区与 Git 策略 |
| [Java 主源码模块一览](./java-modules.md) | 每个 `src/main/java` 下 `.java` 文件的唯一职责（不含测试） |
| [SQLite 访问与并发门闩](./sqlite-access.md) | `desubtitle.db` 与 `SqliteConcurrencyController` 约定 |