package com.pekara.service;

import com.pekara.dto.chat.ChatMessageDto;
import com.pekara.dto.chat.ConversationDto;
import com.pekara.model.Conversation;
import com.pekara.model.Message;
import com.pekara.model.User;
import com.pekara.repository.ConversationRepository;
import com.pekara.repository.MessageRepository;
import com.pekara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatMessageDto sendMessage(ChatMessageDto messageDto) {
        Conversation conversation = conversationRepository.findById(messageDto.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        User sender = userRepository.findByEmail(messageDto.getSenderEmail())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(messageDto.getContent())
                .build();

        Message savedMessage = messageRepository.save(message);
        return mapToMessageDto(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getConversationHistory(Long conversationId) {
        return messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::mapToMessageDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(String email) {
        return conversationRepository.findAllByUserEmail(email)
                .stream()
                .map(this::mapToConversationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationDto getOrCreateConversation(List<String> participantEmails) {
        Set<String> participantEmailSet = new HashSet<>(participantEmails);
        return conversationRepository.findByParticipants(participantEmailSet, participantEmailSet.size())
                .map(this::mapToConversationDto)
                .orElseGet(() -> {
                    Set<User> participants = participantEmails.stream()
                            .map(email -> userRepository.findByEmail(email)
                                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + email)))
                            .collect(Collectors.toSet());
                    Conversation conversation = Conversation.builder()
                            .participants(participants)
                            .build();
                    return mapToConversationDto(conversationRepository.save(conversation));
                });
    }

    private ChatMessageDto mapToMessageDto(Message message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderEmail(message.getSender().getEmail())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private ConversationDto mapToConversationDto(Conversation conversation) {
        ChatMessageDto lastMessage = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(this::mapToMessageDto)
                .orElse(null);

        return ConversationDto.builder()
                .id(conversation.getId())
                .createdAt(conversation.getCreatedAt())
                .participantEmails(conversation.getParticipants().stream().map(User::getEmail).collect(Collectors.toSet()))
                .lastMessage(lastMessage)
                .build();
    }
}
