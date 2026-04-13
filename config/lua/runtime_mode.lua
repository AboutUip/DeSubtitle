-- 运行模式只读参考；与 config/json 中可写项的合并策略由 Java 实现。
-- 禁止在此存放密钥；凭证使用环境变量。

return {
  -- 是否为调试模式：true 时启用调试相关行为（具体语义由 Java 读取后解释）
  debug_mode = false,
}
