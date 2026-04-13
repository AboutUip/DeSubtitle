# `config/yml/application.yml` — Spring Boot 主配置

| 项目 | 说明 |
|------|------|
| 作用 | 承载原 `src/main/resources/application.properties` 中的 Spring 与 `desubtitle.*` 键值；由 classpath 中极简 `application.properties` 通过 **`spring.config.import=file:./config/yml/application.yml`** 加载。 |
| 路径 | 相对 **JVM 工作目录**（`user.dir`）：部署时需从仓库根或等价布局启动，使 `./config/yml/application.yml` 可解析。 |
| 优先级 | 与标准 Spring Boot 一致：`config/lua/ports.lua` 经 `StartupLuaPorts` 注入的默认属性**低于**本文件及命令行参数（见 [lua-ports.md](./lua-ports.md)）。 |
| 密钥 | **禁止**在本文件写入 AccessKey、JWT 密钥等；凭证规则见 [../restriction/hard-constraints.md](../restriction/hard-constraints.md)。 |

## 配置项一览

| 键（YAML 路径） | 类型 / 示例 | 说明 |
|-----------------|-------------|------|
| `spring.application.name` | 字符串 | 应用名（如 `DeSubtitle`）。 |
| `spring.web.resources.add-mappings` | 布尔 | `false`：关闭 Boot 默认 classpath 静态资源映射；站点由 `web/` 目录映射提供。 |
| `spring.datasource.url` | JDBC URL | SQLite 文件路径，默认 `jdbc:sqlite:data/desubtitle.db`（`data/` 在仓库根）。 |
| `spring.datasource.driver-class-name` | 字符串 | `org.sqlite.JDBC`。 |
| `spring.jpa.database-platform` | 字符串 | `org.hibernate.community.dialect.SQLiteDialect`。 |
| `spring.jpa.hibernate.ddl-auto` | 字符串 | 默认 `update`。 |
| `spring.jpa.show-sql` | 布尔 | 默认 `false`。 |
| `spring.servlet.multipart.max-file-size` | 大小 | 单文件上限，默认 `512MB`。 |
| `spring.servlet.multipart.max-request-size` | 大小 | 整请求体上限，默认 `512MB`。 |
| `desubtitle.video.lifecycle-purge-interval-ms` | 长整型 | 过期上传视频清理调度间隔（毫秒），默认 `15000`。 |
| `desubtitle.ui.video-processing-lanes` | 整数 | `GET /life` 的 `videoProcessingLanes`（1–8），默认 `3`。 |
| `desubtitle.aliyun.subtitle-erase-region` | 字符串 | 擦除区域：`full` 或 `bottom` 等，与 [../reference/subtitle-region.md](../reference/subtitle-region.md) 一致，默认 `full`。 |
| `desubtitle.cors.allowed-origins` | 字符串 | 可选；逗号分隔的 CORS 来源。默认不配置即不启用；示例见文件内注释。 |

修改后一般需**重启** Spring Boot 进程生效（无热加载）。

返回配置索引：[README.md](./README.md)。
