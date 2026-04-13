# `config/lua/ports.lua`

仓库内路径：`config/lua/ports.lua`。属 **`config/lua/`**（只读参考、应用不写回；逐项 `--` 注释要求见 [../restriction/code-conventions.md](../restriction/code-conventions.md) §4）。**禁止**存放密钥，见 [../restriction/hard-constraints.md](../restriction/hard-constraints.md)。

## 文件作用

在 Spring 启动前由 `StartupLuaPorts` 解析，将端口写入 `SpringApplication.setDefaultProperties`（优先级**低于** `config/yml/application.yml` 与命令行参数）。

## 文件缺失时

不注入任何端口相关默认项，完全沿用 Spring 与 `config/yml/application.yml` 的配置。

## 配置项

| 键 | 作用 | 影响 | 支持的值 | 默认值（仓库） |
|----|------|------|-----------|----------------|
| `backend_port` | 声明后端 HTTP 监听端口意图 | 成功解析后映射为 Spring 属性 **`server.port`**（字符串形式），决定 **Spring Boot 对外监听端口** | Lua **数字**，解析为整数且须在 **1–65535**；否则启动时在读取阶段抛出 `IllegalStateException` | **8080** |
| `frontend_port` | 声明前端开发服务器或「独立前端源」端口意图 | 成功解析后映射为 Spring 属性 **`desubtitle.frontend.port`**，供将来 CORS、文档生成、脚本等读取；**当前仓库内无其它 Java 代码消费该属性**，不改变后端静态资源托管方式（静态页仍由后端同进程、同 `server.port` 提供） | 同上，**1–65535** 整数 | **5173** |

## 与实现的对齐

`com.kitepromiss.desubtitle.config.StartupLuaPorts`、`LuaConfigLoader`。
