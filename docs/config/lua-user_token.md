# `config/lua/user_token.lua`

仓库内路径：`config/lua/user_token.lua`。属 **`config/lua/`**（只读参考、应用不写回；逐项 `--` 注释要求见 [../restriction/code-conventions.md](../restriction/code-conventions.md) §4）。**禁止**存放 JWT 签名密钥，见 [../restriction/hard-constraints.md](../restriction/hard-constraints.md)；密钥使用环境变量 **`DESUBTITLE_JWT_SECRET`**（UTF-8 下建议至少 **32 字节**，以满足 HS256）。

## 文件作用

控制匿名访客临时 JWT 的**有效期**（分钟）。由 **`GET /getUserToken`** 在每次签发时使用，见 [../api/get-user-token.md](../api/get-user-token.md)。

## 当前加载情况

由 **`UserTokenLuaSettings`** 在每次签发请求时读取；文件缺失或解析失败时使用默认 **60** 分钟，并对数值做上下限夹取。

## 配置项

| 键 | 作用 | 影响 | 支持的值 | 默认值（仓库） |
|----|------|------|-----------|----------------|
| `token_ttl_minutes` | JWT 的 `exp` 相对 `iat` 的时长 | 决定返回体中 `expiresInSeconds`（= 分钟 × 60）；非法或缺失时用内置默认并夹取到 [1, 10080] | Lua **数字**（按分钟解析为整数） | **60** |

## 与实现的对齐

`LuaConfigLoader`；`UserTokenLuaSettings`、`UserJwtIssuerService`。签发时会在 SQLite 表 **`user_tokens`** 插入一行（与 JWT `jti` 对齐），详见 [../api/get-user-token.md](../api/get-user-token.md)。
