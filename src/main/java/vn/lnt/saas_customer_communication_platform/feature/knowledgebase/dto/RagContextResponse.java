package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto;

import java.util.List;

public class RagContextResponse {

    private Long companyId;
    private Long knowledgeBaseId;
    private Long conversationId;
    private String query;
    private boolean hasRelevantKnowledge;
    private double maxSimilarityScore;
    private boolean insufficientContext;
    private boolean humanHandoffRequested;
    private String insufficientReason;
    private List<DocumentChunkSearchResult> matchedChunks;
    private String knowledgeContext;
    private String conversationHistoryContext;
    private String systemInstruction;
    private String fullPrompt;

    public RagContextResponse() {
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isHasRelevantKnowledge() {
        return hasRelevantKnowledge;
    }

    public void setHasRelevantKnowledge(boolean hasRelevantKnowledge) {
        this.hasRelevantKnowledge = hasRelevantKnowledge;
    }

    public double getMaxSimilarityScore() {
        return maxSimilarityScore;
    }

    public void setMaxSimilarityScore(double maxSimilarityScore) {
        this.maxSimilarityScore = maxSimilarityScore;
    }

    public boolean isInsufficientContext() {
        return insufficientContext;
    }

    public void setInsufficientContext(boolean insufficientContext) {
        this.insufficientContext = insufficientContext;
    }

    public boolean isHumanHandoffRequested() {
        return humanHandoffRequested;
    }

    public void setHumanHandoffRequested(boolean humanHandoffRequested) {
        this.humanHandoffRequested = humanHandoffRequested;
    }

    public String getInsufficientReason() {
        return insufficientReason;
    }

    public void setInsufficientReason(String insufficientReason) {
        this.insufficientReason = insufficientReason;
    }

    public List<DocumentChunkSearchResult> getMatchedChunks() {
        return matchedChunks;
    }

    public void setMatchedChunks(List<DocumentChunkSearchResult> matchedChunks) {
        this.matchedChunks = matchedChunks;
    }

    public String getKnowledgeContext() {
        return knowledgeContext;
    }

    public void setKnowledgeContext(String knowledgeContext) {
        this.knowledgeContext = knowledgeContext;
    }

    public String getConversationHistoryContext() {
        return conversationHistoryContext;
    }

    public void setConversationHistoryContext(String conversationHistoryContext) {
        this.conversationHistoryContext = conversationHistoryContext;
    }

    public String getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(String systemInstruction) {
        this.systemInstruction = systemInstruction;
    }

    public String getFullPrompt() {
        return fullPrompt;
    }

    public void setFullPrompt(String fullPrompt) {
        this.fullPrompt = fullPrompt;
    }
}

