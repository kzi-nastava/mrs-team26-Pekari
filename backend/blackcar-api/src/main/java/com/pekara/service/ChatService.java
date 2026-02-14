package com.pekara.service;

import com.pekara.dto.chat.ChatMessageDto;
import com.pekara.dto.chat.ConversationDto;

import java.util.List;

public interface ChatService {
    ChatMessageDto sendMessage(ChatMessageDto messageDto);
    List<ChatMessageDto> getConversationHistory(Long conversationId, String requesterEmail);
    List<ConversationDto> getUserConversations(String email);
    ConversationDto getOrCreateConversation(List<String> participantEmails);
}
