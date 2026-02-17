package com.pekara.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebConversation {
    private Long id;
    private LocalDateTime createdAt;
    private Set<String> participantEmails;
    private WebChatMessage lastMessage;
}
