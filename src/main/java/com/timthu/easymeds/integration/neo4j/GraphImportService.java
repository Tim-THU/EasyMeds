package com.timthu.easymeds.integration.neo4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timthu.easymeds.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GraphImportService {

    private final AppProperties appProperties;
    private final GithubDatasetClient githubDatasetClient;
    private final Neo4jKnowledgeGraphGateway knowledgeGraphGateway;
    private final ObjectMapper objectMapper;

    public GraphImportService(
            AppProperties appProperties,
            GithubDatasetClient githubDatasetClient,
            Neo4jKnowledgeGraphGateway knowledgeGraphGateway,
            ObjectMapper objectMapper
    ) {
        this.appProperties = appProperties;
        this.githubDatasetClient = githubDatasetClient;
        this.knowledgeGraphGateway = knowledgeGraphGateway;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void importIfConfigured() throws IOException, InterruptedException {
        if (!appProperties.getGraphImport().isInitOnStartup()) {
            return;
        }
        if (appProperties.getGraphImport().getDataUrl().isBlank()) {
            throw new IllegalStateException("GRAPH_DATA_URL 未配置");
        }
        String content = githubDatasetClient.fetchText(appProperties.getGraphImport().getDataUrl());
        List<SymptomDiseasePair> pairs = switch (appProperties.getGraphImport().getDataFormat().toUpperCase(Locale.ROOT)) {
            case "JSON" -> parseJson(content);
            default -> parseCsv(content);
        };
        knowledgeGraphGateway.importPairs(pairs);
    }

    private List<SymptomDiseasePair> parseCsv(String content) throws IOException {
        List<SymptomDiseasePair> pairs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return pairs;
            }
            String[] headers = splitCsvLine(headerLine);
            int diseaseIndex = findIndex(headers, appProperties.getGraphImport().getDiseaseField(), "disease");
            int symptomIndex = findIndex(headers, appProperties.getGraphImport().getSymptomField(), "symptoms", "symptom");
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = splitCsvLine(line);
                if (values.length <= Math.max(diseaseIndex, symptomIndex)) {
                    continue;
                }
                String disease = clean(values[diseaseIndex]);
                for (String symptom : splitSymptoms(values[symptomIndex])) {
                    if (disease != null && symptom != null) {
                        pairs.add(new SymptomDiseasePair(symptom, disease));
                    }
                }
            }
        }
        return distinctPairs(pairs);
    }

    private List<SymptomDiseasePair> parseJson(String content) throws IOException {
        List<SymptomDiseasePair> pairs = new ArrayList<>();
        JsonNode root = objectMapper.readTree(content);
        if (!root.isArray()) {
            return pairs;
        }
        for (JsonNode item : root) {
            String disease = readField(item, appProperties.getGraphImport().getDiseaseField(), "disease");
            JsonNode symptomNode = item.path(appProperties.getGraphImport().getSymptomField());
            if (symptomNode.isMissingNode() || symptomNode.isNull()) {
                symptomNode = item.path("symptom");
            }
            if (symptomNode.isArray()) {
                for (JsonNode symptom : symptomNode) {
                    String normalized = clean(symptom.asText());
                    if (disease != null && normalized != null) {
                        pairs.add(new SymptomDiseasePair(normalized, disease));
                    }
                }
            } else {
                for (String symptom : splitSymptoms(symptomNode.asText())) {
                    if (disease != null && symptom != null) {
                        pairs.add(new SymptomDiseasePair(symptom, disease));
                    }
                }
            }
        }
        return distinctPairs(pairs);
    }

    private int findIndex(String[] headers, String... expected) {
        for (int i = 0; i < headers.length; i++) {
            String header = clean(headers[i]);
            for (String candidate : expected) {
                if (candidate.equalsIgnoreCase(header)) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("数据字段不存在: " + String.join(",", expected));
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private List<String> splitSymptoms(String raw) {
        String normalized = raw == null ? "" : raw
                .replace("，", ",")
                .replace("、", ",")
                .replace("；", ",")
                .replace(";", ",")
                .replace("|", ",")
                .replace("/", ",");
        List<String> symptoms = new ArrayList<>();
        for (String token : normalized.split(",")) {
            String value = clean(token);
            if (value != null) {
                symptoms.add(value);
            }
        }
        return symptoms;
    }

    private String readField(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = item.path(fieldName);
            if (!node.isMissingNode() && !node.isNull()) {
                String value = clean(node.asText());
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private List<SymptomDiseasePair> distinctPairs(List<SymptomDiseasePair> pairs) {
        Set<String> seen = new LinkedHashSet<>();
        List<SymptomDiseasePair> result = new ArrayList<>();
        for (SymptomDiseasePair pair : pairs) {
            String key = pair.symptom() + "->" + pair.disease();
            if (seen.add(key)) {
                result.add(pair);
            }
        }
        return result;
    }
}