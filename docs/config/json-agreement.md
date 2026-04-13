# `config/json/agreement.json`

仓库内路径：`config/json/agreement.json`。属 **`config/json/`**（可读写、UTF-8 标准 JSON、无注释）。**禁止**存放密钥，见 [../restriction/hard-constraints.md](../restriction/hard-constraints.md)。

## 文件作用

存放面向用户展示的**协议 / 声明**全文，由 **`GET /getAgreement`** 以纯文本形式返回（见 [../api/get-agreement.md](../api/get-agreement.md)）。你可直接编辑该 JSON，将协议内容写入字段 **`text`**（JSON 字符串，支持换行转义 `\n` 等）。

## 当前加载情况

由 **`AgreementService`** 在每次请求时读取；无专用启动加载。

## 配置项

| 键 | 作用 | 影响 | 支持的值 | 默认值（仓库） |
|----|------|------|-----------|----------------|
| `text` | 协议或声明正文 | 决定 **`GET /getAgreement`** 响应体；根非对象或缺键时视为空串 | JSON **字符串** | **""**（空串） |

## 与实现的对齐

`JsonConfigLoader`、`com.kitepromiss.desubtitle.agreement.AgreementService`。
