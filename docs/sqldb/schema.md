# SQLite 表结构说明

## 1. 库文件与演进方式

| 项 | 值 |
|----|-----|
| JDBC URL | `jdbc:sqlite:data/desubtitle.db`（相对进程工作目录） |
| DDL | `spring.jpa.hibernate.ddl-auto=update`（Hibernate 按实体补表/列） |
| 方言 | `org.hibernate.community.dialect.SQLiteDialect` |

**时间类型**：`java.time.Instant` 在 SQLite 中由 Hibernate 映射为 **`BIGINT`**，语义为 **UTC 毫秒时间戳**（epoch millis）。

**遗留库补齐**：若旧库缺列，启动后由 `UserVideoExpiresAtSqliteMigration`、`UserVideoDesubtitleSqliteMigration` 执行 `ALTER TABLE … ADD COLUMN`（见 `com.kitepromiss.desubtitle.video` 包）。

业务路径上的读写须遵守 [../architectuure/sqlite-access.md](../architectuure/sqlite-access.md) 中的 **`SqliteConcurrencyController`** 约定。

---

## 2. 表 `user_videos`

用户上传视频的元数据与去字幕任务状态；源文件位于 `data/videos/`，成品位于 `data/desubtitle/`（由 `stored_file_name` / `desubtitle_output_file_name` 与磁盘配合）。

| 列名 | SQL 类型（近似） | 可空 | 说明 |
|------|------------------|------|------|
| `id` | `VARCHAR(64)` | 否 | 主键；业务视频 id |
| `user_id` | `VARCHAR(64)` | 否 | 归属用户（与 JWT `sub` 等对齐） |
| `stored_file_name` | `VARCHAR(256)` | 否 | `data/videos/` 下磁盘文件名 |
| `original_file_name` | `VARCHAR(512)` | 是 | 原始上传文件名 |
| `content_type` | `VARCHAR(128)` | 是 | MIME 类型 |
| `size_bytes` | `BIGINT` | 否 | 文件大小（字节） |
| `created_at` | `BIGINT` | 否 | 创建时刻 |
| `expires_at` | `BIGINT` | 否 | 上传记录/源文件逻辑过期时刻 |
| `desubtitle_job_id` | `VARCHAR(128)` | 是 | EraseVideoSubtitles 返回的 `RequestId`，作 `GetAsyncJobResult` 的 `JobId` |
| `desubtitle_last_status` | `VARCHAR(64)` | 是 | 异步 `Data.Status` 或本地阶段状态（如 `SUBMIT_FAILED`） |
| `desubtitle_error` | `VARCHAR(2048)` | 是 | 错误摘要 |
| `desubtitle_output_file_name` | `VARCHAR(256)` | 是 | `data/desubtitle/` 下成品文件名 |
| `desubtitle_output_expires_at` | `BIGINT` | 是 | 成品本地保留到期时刻 |

**实体类**：`com.kitepromiss.desubtitle.video.UserVideoEntity`

---

## 3. 表 `user_tokens`

匿名用户 JWT 持久化行：`jti` 与令牌声明一致，用于校验未撤销、未过期及撤销操作。

| 列名 | SQL 类型（近似） | 可空 | 说明 |
|------|------------------|------|------|
| `jti` | `VARCHAR(64)` | 否 | 主键；JWT ID |
| `user_id` | `VARCHAR(64)` | 否 | 该令牌对应的用户 id |
| `expires_at` | `BIGINT` | 否 | 令牌过期时刻 |
| `revoked` | `BOOLEAN` / 整型 | 否 | 是否已撤销 |
| `created_at` | `BIGINT` | 否 | 写入时刻 |

**实体类**：`com.kitepromiss.desubtitle.user.UserTokenEntity`

**相关 HTTP 说明**：[../api/get-user-token.md](../api/get-user-token.md)、[../api/bearer-user-auth.md](../api/bearer-user-auth.md)

---

## 4. 表 `aliyun_credentials`

非调试模式下由引导流程写入的阿里云 AccessKey；**至多一行**，`id` 固定为 `1`（见实体常量 `SINGLETON_ID`）。

| 列名 | SQL 类型（近似） | 可空 | 说明 |
|------|------------------|------|------|
| `id` | `BIGINT` | 否 | 主键；恒为 `1` |
| `access_key_id` | `VARCHAR(256)` | 否 | AccessKey ID |
| `access_key_secret` | `VARCHAR(512)` | 否 | AccessKey Secret（明文存储，责任边界见参考文档） |

**实体类**：`com.kitepromiss.desubtitle.credential.AliyunCredentialsEntity`

**安全与存储**：[../reference/aliyun-credentials-storage.md](../reference/aliyun-credentials-storage.md)

---

## 5. 维护说明

- 表结构以**运行中的 `data/desubtitle.db`** 与 **当前 `main` 分支实体**为准；若二者不一致，以实体 + 迁移逻辑为源，必要时重建库或执行迁移。
- 新增表或列时：补充 JPA 实体、按需增加 SQLite 迁移组件，并**更新本文**。
