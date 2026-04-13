-- 匿名访客临时 JWT 的只读参考（应用不写回）。
-- 禁止在此存放密钥。JWT 签名密钥优先环境变量 DESUBTITLE_JWT_SECRET（UTF-8 至少 32 字节）；
-- 未设置时进程启动会生成 32 字节随机密钥仅存内存（重启后旧令牌失效，多实例须显式配置统一密钥）。

return {
  -- 每次 GET /getUserToken 签发 token 的有效期，单位：分钟（Java 读取后用于 exp）
  token_ttl_minutes = 60,
}
