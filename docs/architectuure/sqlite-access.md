# SQLite 访问与并发门闩

本应用使用单一文件库 **`data/desubtitle.db`**（见 `spring.datasource.url`）。SQLite 在多线程并发写入时易出现 `database is locked`，因此约定如下。

## `SqliteConcurrencyController`

- 位置：`com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController`。  
- 职责：进程内 **`ReentrantLock` 互斥**，所有与 **`desubtitle.db`** 相关的业务访问须在 `supply` / `run` / `call` 的临界区内完成。

## 当前已接线的调用方（须保持）

| 区域 | 说明 |
|------|------|
| `AliyunCredentialSetupService` | 非调试模式下对 `AliyunCredentialsRepository` 的读写 |
| `InitService` | `DataSource#getConnection` 触库（`SELECT 1`） |
| `UserJwtIssuerService` | 写入 `user_tokens` 并签发 JWT |
| `UserJwtAuthenticationService` | 按 `jti` 查询 `user_tokens` 校验未撤销且未过期 |
| `UserTokenManagementService` | 撤销等写回 `user_tokens` |

新增任何 **JPA Repository 调用**、**JdbcTemplate**、**手写 `DataSource#getConnection()`** 时，**必须**将整段数据库操作包在同一门闩临界区内，禁止在异步线程中绕过门闩访问同一库文件。

## 与框架启动的边界

Spring Data / Hibernate 在**应用上下文启动**阶段可能执行 DDL、元数据查询等，该阶段**不**经过本门闩；属框架初始化短时行为。业务请求路径须遵守上文约定。

## 相关文档

- 表与列一览：[../sqldb/schema.md](../sqldb/schema.md)
- 匿名 JWT 与表 `user_tokens`：[../api/get-user-token.md](../api/get-user-token.md)  
- Bearer 门禁：[../api/bearer-user-auth.md](../api/bearer-user-auth.md)
