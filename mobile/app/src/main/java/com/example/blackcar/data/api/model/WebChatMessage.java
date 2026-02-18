package com.example.blackcar.data.api.model;

public class WebChatMessage {
    private Long id;
    private Long conversationId;
    private String senderEmail;
    private String senderRole;
    private String content;
    private String createdAt;

    public WebChatMessage() {}

    public WebChatMessage(Long conversationId, String senderEmail, String content) {
        this.conversationId = conversationId;
        this.senderEmail = senderEmail;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
