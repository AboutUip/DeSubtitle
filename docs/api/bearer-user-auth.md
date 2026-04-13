# Bearer 匿名用户 JWT（横切）

除明确白名单外，所有 **Spring MVC `HandlerMethod` 控制器** 请求须携带：

```http
Authorization: Bearer <compact JWT>
```

其中 `<compact JWT>` 为 **`GET /getUserToken`** 返回的 `token` 字段（见 [get-user-token.md](./get-user-token.md)）。

## 处理顺序

1. **`InitializationGateInterceptor`**（更靠前）：未完成初始化时返回 **503**。  
2. **`AnonymousUserBearerInterceptor`**：校验 Bearer 与 SQLite 中 `user_tokens` 行状态。

非 `HandlerMethod` 的处理器（如静态资源）**不**经 Bearer 校验。

## 白名单（全局拦截器不要求 `Authorization`）

源码以 **`MvcPublicEndpointRules#allowsWithoutBearer`** 为判定入口。以下 **HandlerMethod** 在通过初始化门禁后**不会**被 **`AnonymousUserBearerInterceptor`** 要求 Bearer（另放行 Spring Boot **`ErrorController`** 实现类，Boot 3/4 按接口全名字符串识别）：

- **`GET /getUserToken`**（获取临时 token）  
- **`GET /init/status`**、**`POST /init/credentials`**、**`POST /init`**（初始化引导）  
- **`GET /life`**（**例外**：不经拦截器验库，但 **`LifeController` 仍要求** 请求携带 **`Authorization: Bearer …`**，自行校验；无效则刷新 token，并返回指标快照，见 [life.md](./life.md)）

**不**在此列的控制器（例如 **`GET /getAgreement`**、**`POST /uploadVideo`**）在初始化完成后**必须**携带有效 **`Authorization: Bearer <JWT>`**（由拦截器 + `UserJwtAuthenticationService` 校验）。

未完成初始化时，上述非白名单路径可能先被 **`InitializationGateInterceptor`** 以 **503** 拦截，尚不会走到 Bearer 校验；一旦初始化完成且无 Bearer（且控制器不在 `allowsWithoutBearer` 中），则返回 **401**。

## 校验内容

1. `Authorization` 头格式为 `Bearer <token>`（`Bearer` 大小写不敏感）。  
2. JWT **签名**有效（密钥 `DESUBTITLE_JWT_SECRET`）。  
3. 表 **`user_tokens`** 中存在对应 **`jti`**，且 **`user_id` 与 `sub` 一致**、`revoked=false`、`expires_at` 未早于当前时间。

## 成功后的请求属性

校验通过后写入请求属性：

| 常量（`AnonymousUserRequestAttributes`） | 类型 | 含义 |
|------------------------------------------|------|------|
| `ANONYMOUS_USER_PRINCIPAL` | `AnonymousUserPrincipal` | `userId`（`sub`）与 `tokenId`（`jti`） |
| `ANONYMOUS_USER_ID` | `String` | 与 `principal.userId()` 相同，便于只读 id 的代码路径 |

`UserDataManager` 等内部组件从 `RequestContextHolder` 读取上述属性。

## 错误响应（**401** `application/json`）

| `error` 值 | 含义 |
|------------|------|
| `missing_bearer_token` | 缺少 `Authorization` 头 |
| `invalid_authorization_header` | 头存在但非 `Bearer …` 格式 |
| `invalid_or_revoked_token` | 验签失败、无对应行、已撤销或已过期 |

## 实现锚点

`MvcPublicEndpointRules`、`AnonymousUserBearerInterceptor`、`UserJwtAuthenticationService`、`AnonymousUserJwtGate`、`InitializationWebConfiguration`（拦截器顺序）。

## 动态撤销

Java API：`UserTokenManagementService#revokeByJti`（须在门闩内，已由该类封装）。亦可直接 SQL 更新 `user_tokens.revoked`（须避免与业务并发冲突，生产侧建议仍走应用 API）。
