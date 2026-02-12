package com.pekara.controller;

import com.pekara.dto.chat.WebChatMessage;
import com.pekara.dto.chat.WebConversation;
import com.pekara.mapper.ChatMapper;
import com.pekara.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "Chat management endpoints")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload WebChatMessage messageDto) {
        var serviceMessage = chatMapper.toServiceMessage(messageDto);
        var savedMessage = chatService.sendMessage(serviceMessage);
        var webMessage = chatMapper.toWebMessage(savedMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + webMessage.getConversationId(), webMessage);
    }

    @GetMapping("/conversations/{email}")
    public ResponseEntity<List<WebConversation>> getUserConversations(@PathVariable String email) {
        var conversations = chatService.getUserConversations(email).stream()
                .map(chatMapper::toWebConversation)
                .toList();
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<WebChatMessage>> getConversationHistory(@PathVariable Long conversationId) {
        var history = chatService.getConversationHistory(conversationId).stream()
                .map(chatMapper::toWebMessage)
                .toList();
        return ResponseEntity.ok(history);
    }

    @PostMapping("/conversations")
    public ResponseEntity<WebConversation> getOrCreateConversation(@RequestBody List<String> participantEmails) {
        var conversation = chatService.getOrCreateConversation(participantEmails);
        return ResponseEntity.ok(chatMapper.toWebConversation(conversation));
    }
}
