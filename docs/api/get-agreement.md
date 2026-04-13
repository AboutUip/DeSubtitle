# `GET /getAgreement` — 用户协议正文（纯文本）

| 项目 | 说明 |
|------|------|
| 作用 | 返回 `config/json/agreement.json` 中 **`text`** 字段内容，供前端展示协议/声明。 |
| 方法与路径 | `GET /getAgreement` |
| 请求 | 无参数、无请求体。 |
| 成功响应 | **200 OK**，`Content-Type: text/plain;charset=UTF-8`，**正文**为协议字符串（可为空）。 |
| 文件缺失或解析失败 | 仍 **200**，正文为**空字符串**。 |
| 门禁 | 未完成初始化时仍允许访问（初始化门禁白名单）。**初始化完成后**须 **`Authorization: Bearer <JWT>`**（见 [bearer-user-auth.md](./bearer-user-auth.md)）。 |

配置说明见 [../config/json-agreement.md](../config/json-agreement.md)。
