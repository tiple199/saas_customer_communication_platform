package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.dto;

import java.util.List;

public class RagAskResponse {

    private String query;
    private String answer;
    private boolean hasRelevantKnowledge;
    private boolean insufficientContext;
    private double maxSimilarityScore;
    private String insufficientReason;
    private Long companyId;
    private Long knowledgeBaseId;
    private Long conversationId;
    private List<DocumentChunkSearchResult> matchedChunks;

    public RagAskResponse() {
    }

    public RagAskResponse(String query, String answer, boolean hasRelevantKnowledge, Long companyId,
                          Long knowledgeBaseId, Long conversationId, List<DocumentChunkSearchResult> matchedChunks) {
        this(query, answer, hasRelevantKnowledge, !hasRelevantKnowledge, 0.0, null,
                companyId, knowledgeBaseId, conversationId, matchedChunks);
    }

    public RagAskResponse(String query, String answer, boolean hasRelevantKnowledge,
                          boolean insufficientContext, double maxSimilarityScore, String insufficientReason,
                          Long companyId, Long knowledgeBaseId, Long conversationId,
                          List<DocumentChunkSearchResult> matchedChunks) {
        this.query = query;
        this.answer = answer;
        this.hasRelevantKnowledge = hasRelevantKnowledge;
        this.insufficientContext = insufficientContext;
        this.maxSimilarityScore = maxSimilarityScore;
        this.insufficientReason = insufficientReason;
        this.companyId = companyId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.conversationId = conversationId;
        this.matchedChunks = matchedChunks;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isHasRelevantKnowledge() {
        return hasRelevantKnowledge;
    }

    public void setHasRelevantKnowledge(boolean hasRelevantKnowledge) {
        this.hasRelevantKnowledge = hasRelevantKnowledge;
    }

    public boolean isInsufficientContext() {
        return insufficientContext;
    }

    public void setInsufficientContext(boolean insufficientContext) {
        this.insufficientContext = insufficientContext;
    }

    public double getMaxSimilarityScore() {
        return maxSimilarityScore;
    }

    public void setMaxSimilarityScore(double maxSimilarityScore) {
        this.maxSimilarityScore = maxSimilarityScore;
    }

    public String getInsufficientReason() {
        return insufficientReason;
    }

    public void setInsufficientReason(String insufficientReason) {
        this.insufficientReason = insufficientReason;
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

    public List<DocumentChunkSearchResult> getMatchedChunks() {
        return matchedChunks;
    }

    public void setMatchedChunks(List<DocumentChunkSearchResult> matchedChunks) {
        this.matchedChunks = matchedChunks;
    }
}

