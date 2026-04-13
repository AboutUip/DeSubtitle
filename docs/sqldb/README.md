# SQLite 数据库文档

| 文档 | 说明 |
|------|------|
| [schema.md](./schema.md) | 库文件位置、表与列（与 JPA 实体及迁移脚本对齐） |

## 相关文档

| 主题 | 路径 |
|------|------|
| 并发门闩与访问约定 | [../architectuure/sqlite-access.md](../architectuure/sqlite-access.md) |
| 数据源与 DDL 策略 | 仓库根 `src/main/resources/application.properties`（`spring.datasource.url`、`spring.jpa.hibernate.ddl-auto`） |
| AccessKey 存储责任 | [../reference/aliyun-credentials-storage.md](../reference/aliyun-credentials-storage.md) |
