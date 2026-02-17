package com.pekara.repository;

import com.pekara.model.Conversation;
import com.pekara.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.email = :email")
    List<Conversation> findAllByUserEmail(@Param("email") String email);

    @Query("SELECT c FROM Conversation c WHERE size(c.participants) = :count AND NOT EXISTS (SELECT p FROM c.participants p WHERE p.email NOT IN :emails)")
    Optional<Conversation> findByParticipants(@Param("emails") Set<String> emails, @Param("count") Integer count);
}
