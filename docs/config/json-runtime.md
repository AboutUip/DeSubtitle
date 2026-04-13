# `config/json/runtime.json`

仓库内路径：`config/json/runtime.json`。属 **`config/json/`**（可读写、UTF-8 标准 JSON、无注释）。**禁止**存放密钥，见 [../restriction/hard-constraints.md](../restriction/hard-constraints.md)。多文件拆分与仓库约定见 [../architectuure/repository-layout.md](../architectuure/repository-layout.md) 中 `config/json/` 说明。

## 文件作用

承载**可持久化**的运行期状态（如向导/初始化是否完成等）。

## 当前加载情况

由 **`InitService`**（`POST /init`）读取并可能在成功初始化后写回 `initialization_completed`；通用读写仍可用 `JsonConfigLoader`。详见 [../api/init.md](../api/init.md)。

## 配置项

| 键 | 作用 | 影响 | 支持的值 | 默认值（仓库） |
|----|------|------|-----------|----------------|
| `initialization_completed` | 表示「应用认为初始化流程是否已完成」 | 为 `true` 时 **`POST /init` 直接跳过**数据区操作；为 `false` 时执行初始化逻辑；在 **`runtime_mode.lua` 的 `debug_mode` 为 `false`** 且初始化成功后会写为 `true`；**debug 模式下不写入** | JSON **布尔** `true` / `false` | **false** |

## 与实现的对齐

`JsonConfigLoader`；`com.kitepromiss.desubtitle.init.InitService`。
