# EasyMeds 人工配置文档

## 1. 你需要手动准备的内容

在运行后端前，需要你手动准备以下配置：

- DeepSeek 官方 API Key
- Neo4j 数据库实例
- 可选的图谱数据集原始地址

本项目固定使用 DeepSeek 官方地址：

`https://api.deepseek.com`

不使用 OpenRouter。

## 2. DeepSeek 配置

需要配置以下环境变量：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
$env:DEEPSEEK_BASE_URL="https://api.deepseek.com"
$env:DEEPSEEK_CHAT_MODEL="deepseek-chat"
$env:DEEPSEEK_REASONER_MODEL="deepseek-reasoner"
```

说明：

- `deepseek-chat` 用于症状抽取和意图识别
- `deepseek-reasoner` 用于主动追问和最终总结

## 3. Neo4j 配置

建议使用 Neo4j AuraDB。

需要配置以下环境变量：

```powershell
$env:NEO4J_URI="neo4j+s://xxxx.databases.neo4j.io"
$env:NEO4J_USERNAME="neo4j"
$env:NEO4J_PASSWORD="你的数据库密码"
$env:NEO4J_DATABASE="neo4j"
```

说明：

- 如果不配置 Neo4j，服务可以启动，但主动问诊时不会真正利用图谱数据
- 完整 MVP 效果依赖 Neo4j 已导入症状-疾病关系

## 4. 图谱初始化配置

如果你希望应用启动时自动从 GitHub 原始地址导入图谱数据，可以额外配置：

```powershell
$env:GRAPH_INIT_ON_STARTUP="true"
$env:GRAPH_DATA_URL="https://raw.githubusercontent.com/你的仓库/你的分支/xxx.csv"
$env:GRAPH_DATA_FORMAT="CSV"
$env:GRAPH_DISEASE_FIELD="disease"
$env:GRAPH_SYMPTOM_FIELD="symptoms"
```

目前支持两种数据格式：

- `CSV`
- `JSON`

字段要求：

- 疾病字段默认 `disease`
- 症状字段默认 `symptoms`

CSV 示例：

```csv
disease,symptoms
普通感冒,咳嗽|流鼻涕|发热
咽炎,咽痛|咳嗽
```

JSON 示例：

```json
[
  {
    "disease": "普通感冒",
    "symptoms": ["咳嗽", "流鼻涕", "发热"]
  }
]
```

## 5. 可调参数

还可以调整以下运行参数：

```powershell
$env:APP_MAX_QUESTION_TURNS="5"
```

当前代码默认最大追问轮数为 `5`。