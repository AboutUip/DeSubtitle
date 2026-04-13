# 硬性约束

## 1. 密钥与配置

- **禁止**将阿里云 AccessKey、密码、令牌写入仓库内任何已跟踪文件（含 `application.properties`、`*.yml`、`config/lua/*.lua`、`config/json/*.json`、前端脚本、测试资源）。
- 凭证以**环境变量**或部署侧密钥管理为准；与阿里云 Java SDK 常见约定一致时使用 `ALIBABA_CLOUD_ACCESS_KEY_ID`、`ALIBABA_CLOUD_ACCESS_KEY_SECRET`（名称以官方文档为准）。若通过本应用 **`POST /init/credentials`** 写入 SQLite，则为**明文**落盘，安全责任见 [../reference/aliyun-credentials-storage.md](../reference/aliyun-credentials-storage.md)。

## 2. 仓库与文件

- **禁止**主动创建 `.gitkeep`、`.keep` 等占位文件，**除非用户明确要求**。
- **禁止**在用户未要求时批量新增 Markdown（例如擅自写 `CHANGELOG`、`CONTRIBUTING`、额外教程）；用户要求写文档时须遵守 [文档编写约束](./documentation-conventions.md)。
- `.gitignore` 内容由用户自行维护时，**勿**在未获指示时填满默认规则。

## 3. 目录约定（摘要）

与 [../architectuure/repository-layout.md](../architectuure/repository-layout.md) 一致，Agent 修改代码时须遵守：

| 区域 | 要点 |
|------|------|
| `web/` | **仅一个** `index.html`（SPA）；图标 `web/icons/`；第三方 JS `web/plugins/`；业务 JS `web/scripts/`；样式 `web/styles/` |
| `src/` | Spring Boot，**仅 API**，不承担主站页面渲染 |
| `config/` | `json/` 可读写；`lua/` 只读参考且**每项键须带专属注释**（见 [代码规范](./code-conventions.md) §4）；密钥仍走环境变量，见 §1 |
| `data/` | 数据库与运行期数据；**是否入 Git 以当前 `.gitignore` 为准**，勿臆测 |
| `docs/` | 项目文档；**本目录 `docs/restriction/`** 专用于对 Agent 的告知与限制 |

## 4. 实现范围

- **只改**当前任务需要的文件；禁止顺带大范围重构、格式化无关文件、删改用户未点名的注释或业务逻辑。
- 新增依赖须在 `pom.xml` 中有明确理由，并与现有 Spring Boot / Java 版本兼容。
- 代码质量、架构与注释要求见 [代码规范](./code-conventions.md)。

## 5. 外部能力

- 调用阿里云视觉智能（如 `EraseVideoSubtitles`、`GetAsyncJobResult`）时，以 [../reference/](../reference/) 与官方文档为准；**勿编造**未在文档中出现的字段名或状态枚举。
