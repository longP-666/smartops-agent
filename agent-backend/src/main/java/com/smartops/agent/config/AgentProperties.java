package com.smartops.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    private final Rag rag = new Rag();
    private final Ai ai = new Ai();
    private final Security security = new Security();

    public Rag getRag() {
        return rag;
    }

    public Ai getAi() {
        return ai;
    }

    public Security getSecurity() {
        return security;
    }

    public static class Rag {
        private int topK = 5;
        private double similarityThreshold = 0.34;
        private int chunkSize = 700;
        private int chunkOverlap = 120;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap() {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }
    }

    public static class Ai {
        private boolean enabled;
        private String baseUrl;
        private String apiKey;
        private String chatModel;
        private int timeoutSeconds = 25;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

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

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Security {
        private String internalApiKey = "dev-api-key";

        public String getInternalApiKey() {
            return internalApiKey;
        }

        public void setInternalApiKey(String internalApiKey) {
            this.internalApiKey = internalApiKey;
        }
    }
}
