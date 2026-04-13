# 协作与交付

## 1. 语言

- 与用户对话默认使用**简体中文**（除非用户改用其他语言）。

## 2. 任务节奏

- 项目倾向**严格一问一答、逐步对齐**：单次回复聚焦当前问题；用户未要求的扩展功能不主动实现。
- 需要分支决策时（技术栈、接口形态、存储策略等），**先问清或列出选项**，避免擅自拍板。

## 3. 执行方式

- 在真实环境中**优先自行执行**可做的命令（构建、测试、依赖检查），不把「请你本地运行某命令」当唯一交付物，除非权限或网络不允许。
- 编写与修改代码时遵守 [代码规范](./code-conventions.md)（企业级质量、架构、需求对齐、注释风格）。
- 说明变更时，对已有代码使用规范 **代码引用**格式（`startLine:endLine:path`），便于在 IDE 中跳转。

## 4. 文档维护

- 编写与修改 `docs/` 下 Markdown 时遵守 [文档编写约束](./documentation-conventions.md)（风格、目录分工、更新策略）。
- 变更 `src/main/java` 下源码时同步维护 [../architectuure/java-modules.md](../architectuure/java-modules.md)（每文件一职简述；不含测试代码说明）。
- 架构级约定变更：更新 `docs/architectuure/`。
- 厂商 API / SDK 变更：更新 `docs/reference/`。
- 对 Agent 的新增约束：写入 `docs/restriction/` 并更新本目录 [README.md](./README.md) 索引。
