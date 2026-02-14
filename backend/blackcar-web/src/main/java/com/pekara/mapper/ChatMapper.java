package com.pekara.mapper;

import com.pekara.dto.chat.ChatMessageDto;
import com.pekara.dto.chat.ConversationDto;
import com.pekara.dto.chat.WebChatMessage;
import com.pekara.dto.chat.WebConversation;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public WebChatMessage toWebMessage(ChatMessageDto dto) {
        if (dto == null) return null;
        return WebChatMessage.builder()
                .id(dto.getId())
                .conversationId(dto.getConversationId())
                .senderEmail(dto.getSenderEmail())
                .senderRole(dto.getSenderRole())
                .content(dto.getContent())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    public ChatMessageDto toServiceMessage(WebChatMessage webDto) {
        if (webDto == null) return null;
        return ChatMessageDto.builder()
                .id(webDto.getId())
                .conversationId(webDto.getConversationId())
                .senderEmail(webDto.getSenderEmail())
                .senderRole(webDto.getSenderRole())
                .content(webDto.getContent())
                .createdAt(webDto.getCreatedAt())
                .build();
    }

    public WebConversation toWebConversation(ConversationDto dto) {
        if (dto == null) return null;
        return WebConversation.builder()
                .id(dto.getId())
                .createdAt(dto.getCreatedAt())
                .participantEmails(dto.getParticipantEmails())
                .lastMessage(toWebMessage(dto.getLastMessage()))
                .build();
    }
}
