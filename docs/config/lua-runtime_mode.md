# `config/lua/runtime_mode.lua`

仓库内路径：`config/lua/runtime_mode.lua`。属 **`config/lua/`**（只读参考、应用不写回；逐项 `--` 注释要求见 [../restriction/code-conventions.md](../restriction/code-conventions.md) §4）。**禁止**存放密钥，见 [../restriction/hard-constraints.md](../restriction/hard-constraints.md)。

## 文件作用

描述运行模式相关**只读参考**（调试开关等）；与 `config/json` 可写项的合并策略由业务 Java 实现。

## 当前加载情况

由 **`InitService`**（`POST /init`）读取 **`debug_mode`**，用于决定是否在初始化结束后写回 `runtime.json` 的 `initialization_completed`。文件缺失或解析失败时按非调试处理。详见 [../api/init.md](../api/init.md)。

## 配置项

| 键 | 作用 | 影响 | 支持的值 | 默认值（仓库） |
|----|------|------|-----------|----------------|
| `debug_mode` | 标记是否处于调试模式 | 为 `true` 时：**`POST /init` 在成功执行标准初始化后不会**将 `initialization_completed` 置为 `true`；为 `false` 时在成功初始化后**会**写回 `true` | Lua **布尔** `true` / `false`（勿用字符串或数字代替，以免合并逻辑歧义） | **false** |

## 与实现的对齐

`LuaConfigLoader`；`com.kitepromiss.desubtitle.init.InitService`。
