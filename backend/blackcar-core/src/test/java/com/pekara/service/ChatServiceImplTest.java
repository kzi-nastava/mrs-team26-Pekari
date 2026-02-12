package com.pekara.service;

import com.pekara.dto.chat.ChatMessageDto;
import com.pekara.dto.chat.ConversationDto;
import com.pekara.model.Conversation;
import com.pekara.model.Message;
import com.pekara.model.User;
import com.pekara.repository.ConversationRepository;
import com.pekara.repository.MessageRepository;
import com.pekara.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User user1;
    private User user2;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).email("user1@test.com").build();
        user2 = User.builder().id(2L).email("user2@test.com").build();
        conversation = Conversation.builder()
                .id(1L)
                .participants(new HashSet<>(Arrays.asList(user1, user2)))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void sendMessage_ShouldSaveAndReturnMessage() {
        ChatMessageDto dto = ChatMessageDto.builder()
                .conversationId(1L)
                .senderEmail("user1@test.com")
                .content("Hello")
                .build();

        Message savedMessage = Message.builder()
                .id(10L)
                .conversation(conversation)
                .sender(user1)
                .content("Hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(userRepository.findByEmail("user1@test.com")).thenReturn(Optional.of(user1));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        ChatMessageDto result = chatService.sendMessage(dto);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Hello", result.getContent());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void getOrCreateConversation_Existing_ShouldReturnExisting() {
        List<String> emails = Arrays.asList("user1@test.com", "user2@test.com");
        when(conversationRepository.findByParticipants(anySet(), eq(2))).thenReturn(Optional.of(conversation));

        ConversationDto result = chatService.getOrCreateConversation(emails);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void getOrCreateConversation_New_ShouldCreateAndReturn() {
        List<String> emails = Arrays.asList("user1@test.com", "user2@test.com");
        when(conversationRepository.findByParticipants(anySet(), eq(2))).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user1@test.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@test.com")).thenReturn(Optional.of(user2));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        ConversationDto result = chatService.getOrCreateConversation(emails);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(conversationRepository).save(any(Conversation.class));
    }
}
