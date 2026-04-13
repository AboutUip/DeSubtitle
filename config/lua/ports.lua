-- 启动前端口配置（由 Java 在 Spring 启动前读取）。
-- 禁止在此存放密钥；凭证使用环境变量。

return {
  -- Spring Boot HTTP 监听端口，注入 server.port
  backend_port = 8080,
  -- 前端静态页或开发服务器端口，注入 desubtitle.frontend.port
  frontend_port = 5173,
}
