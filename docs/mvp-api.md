# EasyMeds MVP API 文档

## 1. 文档范围

本接口文档仅覆盖 MVP 阶段前端开发需要接入的后端能力：

- 发起一轮新的主动问诊
- 在已有问诊会话中继续回复
- 获取会话详情与历史消息
- 主动结束问诊并拿到最终结果
- 健康检查

不包含用户登录、权限体系、支付、病例长期存储、管理后台等非 MVP 功能。

## 2. 基本约定

- Base URL：`/api/v1`
- 数据格式：`application/json`
- 字符编码：`UTF-8`
- 鉴权方式：MVP 阶段不做登录鉴权
- 会话标识：`session_id`，由后端生成，建议使用 UUID
- 最大主动追问轮数：`5`

## 3. 状态说明

`status` 字段只会出现以下两种值：

- `QUESTIONING`：问诊进行中，前端继续展示输入框
- `COMPLETED`：问诊结束，前端展示最终诊断结果并禁用继续发送

`assistant_message.type` 字段只会出现以下两种值：

- `FOLLOW_UP`：继续追问
- `FINAL_SUMMARY`：输出最终总结

## 4. 通用响应结构

成功响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

失败响应：

```json
{
  "code": "SESSION_NOT_FOUND",
  "message": "会话不存在",
  "data": null
}
```

常见错误码：

- `INVALID_PARAM`：参数错误
- `SESSION_NOT_FOUND`：会话不存在
- `SESSION_COMPLETED`：会话已结束，不能继续发送消息
- `MODEL_SERVICE_ERROR`：大模型服务调用失败
- `GRAPH_QUERY_ERROR`：知识图谱查询失败
- `INTERNAL_ERROR`：服务内部异常

## 5. 数据对象

### 5.1 Message

```json
{
  "role": "assistant",
  "content": "请问您这两天有发热吗？",
  "type": "FOLLOW_UP",
  "created_at": "2026-03-24T10:00:00Z"
}
```

字段说明：

- `role`：`user` 或 `assistant`
- `content`：消息内容
- `type`：仅 `assistant` 消息有值，取值见上文
- `created_at`：消息时间，ISO 8601 格式

### 5.2 FinalResult

```json
{
  "diagnoses": [
    {
      "name": "上呼吸道感染",
      "confidence": "HIGH"
    },
    {
      "name": "普通感冒",
      "confidence": "MEDIUM"
    }
  ],
  "medical_advice": "建议近期前往呼吸内科或全科门诊进一步检查。",
  "medication_advice": "可在医生指导下考虑常见对症药物，避免自行长期用药。",
  "risk_notice": "如果持续高热、呼吸困难或症状明显加重，请尽快线下就医。"
}
```

字段说明：

- `diagnoses`：最终候选诊断列表，MVP 建议最多返回 3 个
- `confidence`：`HIGH`、`MEDIUM`、`LOW`
- `medical_advice`：就医建议
- `medication_advice`：初步用药建议
- `risk_notice`：风险提示

## 6. 接口列表

### 6.1 发起问诊

`POST /consultations`

说明：用户首次输入症状时调用，后端创建新会话并返回 Agent 的第一条回复。

请求参数：

```json
{
  "message": "我最近三天一直咳嗽，还有点头痛"
}
```

请求字段：

- `message`：用户首轮输入，必填，自然语言文本

成功响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "session_id": "b7c8a1d9-47c6-4cd5-b938-7b3e7d07b941",
    "status": "QUESTIONING",
    "turn_count": 1,
    "confirmed_symptoms": [
      "咳嗽",
      "头痛"
    ],
    "assistant_message": {
      "role": "assistant",
      "content": "请问您这几天有发热或者流鼻涕吗？",
      "type": "FOLLOW_UP",
      "created_at": "2026-03-24T10:00:00Z"
    },
    "final_result": null
  }
}
```

前端处理建议：

- 保存 `session_id`
- 将用户消息和 `assistant_message` 一起渲染到聊天区
- 若 `status=COMPLETED`，直接展示 `final_result`

### 6.2 继续问诊

`POST /consultations/{session_id}/messages`

说明：用户回答 Agent 追问，或继续补充症状时调用。

请求参数：

```json
{
  "message": "有一点低烧，但是没有流鼻涕"
}
```

成功响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "session_id": "b7c8a1d9-47c6-4cd5-b938-7b3e7d07b941",
    "status": "QUESTIONING",
    "turn_count": 2,
    "confirmed_symptoms": [
      "咳嗽",
      "头痛",
      "低热"
    ],
    "assistant_message": {
      "role": "assistant",
      "content": "请问您是否伴随喉咙痛或者咽干？",
      "type": "FOLLOW_UP",
      "created_at": "2026-03-24T10:01:00Z"
    },
    "final_result": null
  }
}
```

问诊结束响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "session_id": "b7c8a1d9-47c6-4cd5-b938-7b3e7d07b941",
    "status": "COMPLETED",
    "turn_count": 4,
    "confirmed_symptoms": [
      "咳嗽",
      "头痛",
      "低热",
      "咽痛"
    ],
    "assistant_message": {
      "role": "assistant",
      "content": "根据当前症状，初步更倾向于上呼吸道感染，以下是总结建议。",
      "type": "FINAL_SUMMARY",
      "created_at": "2026-03-24T10:03:00Z"
    },
    "final_result": {
      "diagnoses": [
        {
          "name": "上呼吸道感染",
          "confidence": "HIGH"
        },
        {
          "name": "普通感冒",
          "confidence": "MEDIUM"
        }
      ],
      "medical_advice": "建议近期前往呼吸内科或全科门诊进一步检查。",
      "medication_advice": "可在医生指导下考虑常见对症药物，避免自行长期用药。",
      "risk_notice": "如果持续高热、呼吸困难或症状明显加重，请尽快线下就医。"
    }
  }
}
```

### 6.3 获取会话详情

`GET /consultations/{session_id}`

说明：前端刷新页面、重进会话页时调用，用于恢复聊天记录和当前状态。

成功响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "session_id": "b7c8a1d9-47c6-4cd5-b938-7b3e7d07b941",
    "status": "QUESTIONING",
    "turn_count": 2,
    "confirmed_symptoms": [
      "咳嗽",
      "头痛",
      "低热"
    ],
    "messages": [
      {
        "role": "user",
        "content": "我最近三天一直咳嗽，还有点头痛",
        "created_at": "2026-03-24T10:00:00Z"
      },
      {
        "role": "assistant",
        "content": "请问您这几天有发热或者流鼻涕吗？",
        "type": "FOLLOW_UP",
        "created_at": "2026-03-24T10:00:00Z"
      },
      {
        "role": "user",
        "content": "有一点低烧，但是没有流鼻涕",
        "created_at": "2026-03-24T10:01:00Z"
      },
      {
        "role": "assistant",
        "content": "请问您是否伴随喉咙痛或者咽干？",
        "type": "FOLLOW_UP",
        "created_at": "2026-03-24T10:01:00Z"
      }
    ],
    "final_result": null
  }
}
```

### 6.4 主动结束问诊

`POST /consultations/{session_id}/finish`

说明：用户点击“结束问诊”时调用。后端基于当前已收集信息直接生成最终总结。

请求参数：

```json
{
  "reason": "USER_FINISH"
}
```

成功响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "session_id": "b7c8a1d9-47c6-4cd5-b938-7b3e7d07b941",
    "status": "COMPLETED",
    "turn_count": 2,
    "assistant_message": {
      "role": "assistant",
      "content": "问诊已结束，以下是基于当前信息的初步总结。",
      "type": "FINAL_SUMMARY",
      "created_at": "2026-03-24T10:02:00Z"
    },
    "final_result": {
      "diagnoses": [
        {
          "name": "普通感冒",
          "confidence": "MEDIUM"
        }
      ],
      "medical_advice": "建议继续观察症状变化，如加重请及时线下就医。",
      "medication_advice": "如需用药，请优先遵循医生或药师建议。",
      "risk_notice": "本结果仅供初步参考，不替代专业面诊。"
    }
  }
}
```

### 6.5 健康检查

`GET /health`

说明：用于前端联调时判断服务是否可用。

成功响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "status": "UP"
  }
}
```

## 7. 前端联调流程建议

1. 用户首次发送症状，调用 `POST /consultations`
2. 保存后端返回的 `session_id`
3. 后续每次用户输入都调用 `POST /consultations/{session_id}/messages`
4. 页面刷新后调用 `GET /consultations/{session_id}` 恢复历史记录
5. 用户点击结束按钮时调用 `POST /consultations/{session_id}/finish`
6. 当接口返回 `status=COMPLETED` 时，展示 `final_result`

## 8. 非功能说明

- 前端不需要感知后端具体使用 DeepSeek 直连还是 OpenRouter 路由，统一按本接口协议开发即可
- 后端内部会基于知识图谱检索结果和大模型推理生成追问，但这些中间推理过程不对前端透出
- MVP 阶段建议前端只展示最终诊断、建议和风险提示，不展示复杂置信度计算细节

