# `GET /getUserToken` — 匿名访客临时 JWT

用于**无登录、无注册**场景下区分用户：每次调用签发新的 JWT，并在 SQLite 表 **`user_tokens`** 中插入一行，便于**撤销、审计与运维动态管理**。

| 项目 | 说明 |
|------|------|
| 作用 | 在 **`SqliteConcurrencyController`** 临界区内写入 `user_tokens`，并返回 **HS256 JWT** 与元数据。后续访问受保护接口须带 **`Authorization: Bearer …`**，见 [bearer-user-auth.md](./bearer-user-auth.md)。 |
| 方法与路径 | `GET /getUserToken` |
| 请求 | 无参数、无请求体。 |
| 成功响应 | **200 OK**，`Content-Type: application/json` |
| 成功体字段 | `token`（string）：完整 JWT；`expiresInSeconds`（number）：与配置 TTL 一致；`userId`（string）：与 JWT **`sub`** 相同。 |
| JWT 声明 | **`sub`**：匿名用户 UUID；**`jti`**：本 token 唯一 id，与表主键一致；**`iat` / `exp`**：签发与过期时间。 |
| 密钥未配置或过短 | **503**，体 `{"error":"jwt_secret_not_configured"}`；须设置 **`DESUBTITLE_JWT_SECRET`**（UTF-8 下至少 32 字节）。 |
| 有效期 | 由 **`config/lua/user_token.lua`** 的 **`token_ttl_minutes`**（分钟）决定，见 [../config/lua-user_token.md](../config/lua-user_token.md)。 |
| 初始化门禁 | 未完成初始化时仍允许访问（初始化门禁白名单）。 |
| Bearer | **无需** Bearer（与 init 引导接口并列，见 [bearer-user-auth.md](./bearer-user-auth.md)）。 |

## 表 `user_tokens`（JPA：`UserTokenEntity`）

| 列 | 说明 |
|----|------|
| `jti` | 主键，与 JWT `jti` 一致 |
| `user_id` | 与 JWT `sub` 一致 |
| `expires_at` | 过期时刻（与 JWT `exp` 对齐） |
| `revoked` | 是否撤销；为 true 时 Bearer 校验失败 |
| `created_at` | 创建时刻 |

Hibernate `ddl-auto=update` 会在启动时建表。直接改库可实现批量失效等运维操作；并发写入仍应通过应用与 **`SqliteConcurrencyController`**，见 [../architectuure/sqlite-access.md](../architectuure/sqlite-access.md)。

## 安全说明

- 匿名 JWT **不是**强身份认证：掌握 `DESUBTITLE_JWT_SECRET` 即可签发；表记录用于**服务端可控**的失效与核对。  
- 生产环境须保护密钥；轮换密钥后旧签名全部失效。

## 实现锚点

`UserTokenController`、`UserJwtIssuerService`、`UserTokenRepository`、`SqliteConcurrencyController`、`UserJwtSignatureSupport`。
