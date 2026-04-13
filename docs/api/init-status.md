# `GET /init/status` — 初始化与凭证配置状态

| 项目 | 说明 |
|------|------|
| 作用 | 供前端在未完成初始化时展示引导页（是否已配置 AccessKey、是否调试模式、是否已标完成）。 |
| 方法与路径 | `GET /init/status` |
| 请求 | 无参数、无请求体。 |
| 成功响应 | **200 OK**，JSON 对象，字段见下表。 |
| 门禁 | 未完成初始化时仍允许访问（与 `POST /init`、`POST /init/credentials` 同属白名单）。 |

## 响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `initialized` | boolean | `config/json/runtime.json` 中 `initialization_completed` 是否为 `true`（与初始化门禁缓存一致）。 |
| `debugMode` | boolean | `config/lua/runtime_mode.lua` 中 `debug_mode`。 |
| `credentialsConfigured` | boolean | 非调试：SQLite 中已存在有效 AccessKey 行；调试：进程内存中已提交过非空密钥。 |

典型引导顺序：`initialized == false` → 若 `credentialsConfigured == false` 则展示表单 → 用户 **`POST /init/credentials`** → 再 **`POST /init`**。

详见 [init.md](./init.md)、[init-credentials.md](./init-credentials.md)。
