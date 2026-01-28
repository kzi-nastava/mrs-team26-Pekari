package com.pekara.repository;

import com.pekara.model.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {

    Optional<AccountActivationToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM AccountActivationToken t WHERE t.expiresAt < :now AND t.activatedAt IS NULL")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
