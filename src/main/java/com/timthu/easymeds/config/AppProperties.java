package com.timthu.easymeds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Consultation consultation = new Consultation();
    private final DeepSeek deepseek = new DeepSeek();
    private final Neo4j neo4j = new Neo4j();
    private final GraphImport graphImport = new GraphImport();

    public Consultation getConsultation() {
        return consultation;
    }

    public DeepSeek getDeepseek() {
        return deepseek;
    }

    public Neo4j getNeo4j() {
        return neo4j;
    }

    public GraphImport getGraphImport() {
        return graphImport;
    }

    public static class Consultation {
        private int maxQuestionTurns = 5;

        public int getMaxQuestionTurns() {
            return maxQuestionTurns;
        }

        public void setMaxQuestionTurns(int maxQuestionTurns) {
            this.maxQuestionTurns = maxQuestionTurns;
        }
    }

    public static class DeepSeek {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String chatModel = "deepseek-chat";
        private String reasonerModel = "deepseek-reasoner";
        private final MaxTokens maxTokens = new MaxTokens();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getReasonerModel() {
            return reasonerModel;
        }

        public void setReasonerModel(String reasonerModel) {
            this.reasonerModel = reasonerModel;
        }

        public MaxTokens getMaxTokens() {
            return maxTokens;
        }
    }

    public static class MaxTokens {
        private int extraction = 600;
        private int planning = 1200;

        public int getExtraction() {
            return extraction;
        }

        public void setExtraction(int extraction) {
            this.extraction = extraction;
        }

        public int getPlanning() {
            return planning;
        }

        public void setPlanning(int planning) {
            this.planning = planning;
        }
    }

    public static class Neo4j {
        private String uri = "";
        private String username = "";
        private String password = "";
        private String database = "neo4j";

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }
    }

    public static class GraphImport {
        private boolean initOnStartup;
        private String dataUrl = "";
        private String dataFormat = "CSV";
        private String diseaseField = "disease";
        private String symptomField = "symptoms";
        private int candidateLimit = 5;
        private int symptomLimit = 8;

        public boolean isInitOnStartup() {
            return initOnStartup;
        }

        public void setInitOnStartup(boolean initOnStartup) {
            this.initOnStartup = initOnStartup;
        }

        public String getDataUrl() {
            return dataUrl;
        }

        public void setDataUrl(String dataUrl) {
            this.dataUrl = dataUrl;
        }

        public String getDataFormat() {
            return dataFormat;
        }

        public void setDataFormat(String dataFormat) {
            this.dataFormat = dataFormat;
        }

        public String getDiseaseField() {
            return diseaseField;
        }

        public void setDiseaseField(String diseaseField) {
            this.diseaseField = diseaseField;
        }

        public String getSymptomField() {
            return symptomField;
        }

        public void setSymptomField(String symptomField) {
            this.symptomField = symptomField;
        }

        public int getCandidateLimit() {
            return candidateLimit;
        }

        public void setCandidateLimit(int candidateLimit) {
            this.candidateLimit = candidateLimit;
        }

        public int getSymptomLimit() {
            return symptomLimit;
        }

        public void setSymptomLimit(int symptomLimit) {
            this.symptomLimit = symptomLimit;
        }
    }
}
