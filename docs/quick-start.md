# EasyMeds 快速启动说明

## 1. 环境要求

- JDK 21
- Maven 3.9+
- 可访问 DeepSeek 官方 API
- 可选：Neo4j AuraDB

## 2. 启动前配置

先在 PowerShell 中设置最基本的环境变量：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
$env:DEEPSEEK_BASE_URL="https://api.deepseek.com"
```

如果你已经准备好 Neo4j，再补充：

```powershell
$env:NEO4J_URI="neo4j+s://xxxx.databases.neo4j.io"
$env:NEO4J_USERNAME="neo4j"
$env:NEO4J_PASSWORD="你的数据库密码"
```

## 3. 启动项目

在项目根目录执行：

```powershell
mvn spring-boot:run
```

默认端口：

`8080`

## 4. 健康检查

启动成功后访问：

```text
GET http://localhost:8080/health
```

预期响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "status": "UP"
  }
}
```

## 5. 最小联调顺序

1. 发起问诊

```text
POST http://localhost:8080/api/v1/consultations
Content-Type: application/json
```

请求体：

```json
{
  "message": "我最近三天一直咳嗽，还有点头痛"
}
```

2. 继续问诊

```text
POST http://localhost:8080/api/v1/consultations/{sessionId}/messages
Content-Type: application/json
```

3. 获取会话详情

```text
GET http://localhost:8080/api/v1/consultations/{sessionId}
```

4. 主动结束问诊

```text
POST http://localhost:8080/api/v1/consultations/{sessionId}/finish
Content-Type: application/json
```

## 6. 自动导入图谱

如果你已经有 GitHub 上的原始数据地址，可以在启动前加上：

```powershell
$env:GRAPH_INIT_ON_STARTUP="true"
$env:GRAPH_DATA_URL="https://raw.githubusercontent.com/你的仓库/你的分支/xxx.csv"
$env:GRAPH_DATA_FORMAT="CSV"
```

然后再执行：

```powershell
mvn spring-boot:run
```

应用会在启动阶段自动导入图谱到 Neo4j。