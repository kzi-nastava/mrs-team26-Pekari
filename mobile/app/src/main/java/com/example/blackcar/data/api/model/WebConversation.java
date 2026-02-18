package com.example.blackcar.data.api.model;

import java.util.Set;

public class WebConversation {
    private Long id;
    private String createdAt;
    private Set<String> participantEmails;
    private WebChatMessage lastMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Set<String> getParticipantEmails() { return participantEmails; }
    public void setParticipantEmails(Set<String> participantEmails) { this.participantEmails = participantEmails; }

    public WebChatMessage getLastMessage() { return lastMessage; }
    public void setLastMessage(WebChatMessage lastMessage) { this.lastMessage = lastMessage; }
}
