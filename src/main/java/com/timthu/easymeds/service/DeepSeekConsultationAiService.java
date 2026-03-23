package com.timthu.easymeds.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timthu.easymeds.config.AppProperties;
import com.timthu.easymeds.domain.CandidateDisease;
import com.timthu.easymeds.domain.ConsultationPlan;
import com.timthu.easymeds.domain.ConsultationSession;
import com.timthu.easymeds.domain.DiagnosisItem;
import com.timthu.easymeds.domain.FinalResult;
import com.timthu.easymeds.domain.GraphContext;
import com.timthu.easymeds.domain.GraphSymptomHint;
import com.timthu.easymeds.domain.UserInputAnalysis;
import com.timthu.easymeds.domain.UserIntent;
import com.timthu.easymeds.exception.ApiException;
import com.timthu.easymeds.integration.deepseek.DeepSeekClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekConsultationAiService implements ConsultationAiService {

    private final DeepSeekClient deepSeekClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public DeepSeekConsultationAiService(DeepSeekClient deepSeekClient, AppProperties appProperties, ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserInputAnalysis analyzeUserInput(ConsultationSession session, String message) {
        String systemPrompt = """
                你是一个医疗问诊 NLU 助手。你必须输出 json。
                只返回一个 json object，格式如下：
                {
                  "intent": "SYMPTOM_STATEMENT",
                  "presentSymptoms": [],
                  "absentSymptoms": [],
                  "unknownSymptoms": [],
                  "shouldFinish": false
                }
                规则：
                1. intent 只能取 SYMPTOM_STATEMENT、FOLLOW_UP_ANSWER、FINISH。
                2. 请把用户口语表达映射成规范中文症状词，如“头有点炸”映射为“头痛”。
                3. 如果用户表达“结束问诊、不想继续、先这样”等，intent=FINISH，shouldFinish=true。
                4. 如果用户明确否认某症状，放到 absentSymptoms。
                5. 如果用户回答“不清楚、不知道”，放到 unknownSymptoms。
                6. 不要输出任何 json 之外的文字。
                """;

        String userPrompt = """
                请基于以下上下文解析用户输入，并返回 json：
                当前待确认症状：%s
                已确认症状：%s
                已否认症状：%s
                上一条助手消息：%s
                用户最新输入：%s
                """.formatted(
                defaultText(session.getPendingSymptom()),
                session.getConfirmedSymptoms(),
                session.getDeniedSymptoms(),
                session.getLastAssistantMessage() == null ? "无" : session.getLastAssistantMessage().content(),
                message
        );

        String content = deepSeekClient.requestJson(
                appProperties.getDeepseek().getChatModel(),
                systemPrompt,
                userPrompt,
                appProperties.getDeepseek().getMaxTokens().getExtraction()
        );

        try {
            JsonNode jsonNode = objectMapper.readTree(content);
            return new UserInputAnalysis(
                    UserIntent.valueOf(jsonNode.path("intent").asText("SYMPTOM_STATEMENT")),
                    readStringArray(jsonNode.path("presentSymptoms")),
                    readStringArray(jsonNode.path("absentSymptoms")),
                    readStringArray(jsonNode.path("unknownSymptoms")),
                    jsonNode.path("shouldFinish").asBoolean(false)
            );
        } catch (IOException | IllegalArgumentException ex) {
            throw new ApiException("MODEL_SERVICE_ERROR", "解析 DeepSeek NLU 输出失败", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public ConsultationPlan planNextStep(ConsultationSession session, GraphContext graphContext, boolean forceComplete) {
        String systemPrompt = """
                你是一个基于知识图谱的主动问诊专家。你必须输出 json。
                只返回一个 json object，格式如下：
                {
                  "action": "ASK_FOLLOW_UP",
                  "question": "请问您有发热吗？",
                  "targetSymptom": "发热",
                  "diagnoses": [
                    {"name": "上呼吸道感染", "confidence": "HIGH"}
                  ],
                  "medicalAdvice": "",
                  "medicationAdvice": "",
                  "riskNotice": ""
                }
                规则：
                1. action 只能取 ASK_FOLLOW_UP 或 COMPLETE。
                2. 如果 forceComplete=true，必须输出 COMPLETE。
                3. 如果未达到足够把握且还有可区分症状，优先输出 ASK_FOLLOW_UP。
                4. 如果输出 ASK_FOLLOW_UP，question 必须是自然中文问句，targetSymptom 必须是一个具体症状。
                5. 如果输出 COMPLETE，必须填充 diagnoses、medicalAdvice、medicationAdvice、riskNotice。
                6. 诊断结论最多 3 个，confidence 只能取 HIGH、MEDIUM、LOW。
                7. 不要输出任何 json 之外的文字。
                """;

        String userPrompt = """
                请根据以下问诊状态做决策，并返回 json：
                forceComplete=%s
                当前轮数=%s
                最大轮数=%s
                已确认症状=%s
                已否认症状=%s
                不确定症状=%s
                候选疾病=%s
                未确认症状提示=%s
                """.formatted(
                forceComplete,
                session.getTurnCount(),
                appProperties.getConsultation().getMaxQuestionTurns(),
                session.getConfirmedSymptoms(),
                session.getDeniedSymptoms(),
                session.getUnknownSymptoms(),
                renderDiseases(graphContext.candidateDiseases()),
                renderSymptomHints(graphContext.unconfirmedSymptoms())
        );

        String content = deepSeekClient.requestJson(
                appProperties.getDeepseek().getReasonerModel(),
                systemPrompt,
                userPrompt,
                appProperties.getDeepseek().getMaxTokens().getPlanning()
        );

        try {
            JsonNode jsonNode = objectMapper.readTree(content);
            String action = jsonNode.path("action").asText("COMPLETE");
            if ("ASK_FOLLOW_UP".equals(action) && !forceComplete) {
                String question = jsonNode.path("question").asText("");
                String targetSymptom = jsonNode.path("targetSymptom").asText("");
                if (!question.isBlank()) {
                    return ConsultationPlan.ask(question, targetSymptom);
                }
            }

            return ConsultationPlan.complete(new FinalResult(
                    readDiagnoses(jsonNode.path("diagnoses")),
                    jsonNode.path("medicalAdvice").asText("建议结合线下门诊进一步评估。"),
                    jsonNode.path("medicationAdvice").asText("如需用药，请优先遵循医生或药师建议。"),
                    jsonNode.path("riskNotice").asText("本结果仅供初步参考，不替代专业面诊。")
            ));
        } catch (IOException ex) {
            throw new ApiException("MODEL_SERVICE_ERROR", "解析 DeepSeek 规划输出失败", HttpStatus.BAD_GATEWAY);
        }
    }

    private List<String> readStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private List<DiagnosisItem> readDiagnoses(JsonNode node) {
        List<DiagnosisItem> diagnoses = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String name = item.path("name").asText("");
                String confidence = item.path("confidence").asText("LOW");
                if (!name.isBlank()) {
                    diagnoses.add(new DiagnosisItem(name, confidence));
                }
            }
        }
        if (diagnoses.isEmpty()) {
            diagnoses.add(new DiagnosisItem("待进一步判断", "LOW"));
        }
        return diagnoses;
    }

    private String renderDiseases(List<CandidateDisease> candidateDiseases) {
        if (candidateDiseases.isEmpty()) {
            return "无";
        }
        StringBuilder builder = new StringBuilder();
        for (CandidateDisease disease : candidateDiseases) {
            builder.append(disease.name())
                    .append("(matchedCount=")
                    .append(disease.matchedCount())
                    .append(", matchedSymptoms=")
                    .append(disease.matchedSymptoms())
                    .append("); ");
        }
        return builder.toString();
    }

    private String renderSymptomHints(List<GraphSymptomHint> symptomHints) {
        if (symptomHints.isEmpty()) {
            return "无";
        }
        StringBuilder builder = new StringBuilder();
        for (GraphSymptomHint hint : symptomHints) {
            builder.append(hint.name())
                    .append("(supportCount=")
                    .append(hint.supportCount())
                    .append(", relatedDiseases=")
                    .append(hint.relatedDiseases())
                    .append("); ");
        }
        return builder.toString();
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }
}