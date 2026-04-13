# 引导页提交的阿里云 AccessKey 存储

## 1. 存储位置与形态

| 模式 | `runtime_mode.lua` 中 `debug_mode` | AccessKey 存放位置 |
|------|-------------------------------------|---------------------|
| 正常运行 | `false`（或未开调试） | SQLite 表 **`aliyun_credentials`**（与业务库同文件，默认 `data/desubtitle.db`） |
| 调试 | `true` | **仅进程内存**（`InMemoryAliyunCredentialHolder`），**不写库**；进程退出即丢失 |

字段为 **明文**：`access_key_id`、`access_key_secret` 不以应用层加密存储。

## 2. 安全责任边界

- **本仓库假设**：部署环境由**运维/部署团队**保证主机与磁盘安全（访问控制、备份策略、合规审计等）。
- **密钥保密、泄露后果与合规**由部署方负责；不在应用内实现加密或 HSM 集成（除非后续单独需求）。
- 传输层须使用 **HTTPS**（由反向代理或网关终止 TLS 亦可），避免在公网明文提交密钥。

## 3. 与「环境变量凭证」的关系

调用阿里云 OpenAPI 时仍可使用环境变量 **`ALIBABA_CLOUD_ACCESS_KEY_ID`** / **`ALIBABA_CLOUD_ACCESS_KEY_SECRET`**（见 [sdk-endpoint-auth.md](./sdk-endpoint-auth.md)）。本应用通过引导页写入 SQLite/内存的凭证，供后续在 **Java 内构造客户端**时使用；二者是否并存、以何者优先，由后续业务代码约定。

## 4. 初始化与 `data/` 清理

执行 `POST /init` 且 **`data/` 已存在**时，会清空目录内文件，但**保留**主库文件名 **`desubtitle.db`**，避免误删已写入的 AccessKey 行。其它文件名下的数据仍可能被删除，部署时勿将长期机密仅依赖未列入备份的临时文件。
