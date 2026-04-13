# 用户数据管理器（`UserDataManager`）

进程内、**纯内存**的多用户数据分区与匿名会话辅助，供业务 Service 注入使用。与 SQLite 中的 `user_tokens` **解耦**：内存态随 JVM 退出丢失；撤销会话仍走 `UserTokenManagementService` / SQLite。

## 定位

| 能力 | 说明 |
|------|------|
| 当前用户 | 从 `RequestContextHolder` + `HttpServletRequest` 读取 Bearer 拦截器写入的属性（见 [../api/bearer-user-auth.md](../api/bearer-user-auth.md)）。 |
| 分区存储 | `userId` → `domain` → `key` → `value`，底层均为 `ConcurrentHashMap`；`get*` **不会**凭空创建空分区。 |
| Token | `revokeCurrentUserToken()` 使用请求中的 **`jti`**（`AnonymousUserPrincipal.tokenId()`）标记 `user_tokens.revoked`。 |

## 主要 API（`com.kitepromiss.desubtitle.userdata.UserDataManager`）

- `Optional<String> currentUserId()` / `String requireUserId()`  
- `Optional<AnonymousUserPrincipal> currentPrincipal()` / `AnonymousUserPrincipal requirePrincipal()`  
- `putState` / `getState` / `getOrCreateState` / `computeState` / `removeState`（重载含显式 `userId`，便于非请求线程）  
- `clearDomain` / `clearAllUserData` / `snapshotDomain`  
- `revokeCurrentUserToken()`  

`require*` 在无请求上下文或未设置匿名用户时抛 **`NoCurrentUserContextException`**。

## 并发说明

- 同一 `(userId, domain, key)` 的 `getOrCreateState` 依赖 `ConcurrentHashMap#computeIfAbsent`，多线程下 `Supplier` 对同一 key **至多执行一次**。  
- `computeState` 使用 `ConcurrentHashMap#compute`，原子更新。  
- 异步任务中**没有** `RequestContextHolder` 时，请使用带 **`userId` 参数**的重载，并由调用方传入从上游消息中取得的用户标识。

## 实现锚点

`UserDataManager`、`AnonymousUserPrincipal`、`AnonymousUserRequestAttributes`、`UserJwtAuthenticationService`（返回 `AnonymousUserPrincipal`）。
