package com.kitepromiss.desubtitle.video;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserVideoRepository extends JpaRepository<UserVideoEntity, String> {

    long countByUserId(String userId);

    List<UserVideoEntity> findByExpiresAtBefore(Instant instant);

    List<UserVideoEntity> findByUserIdOrderByCreatedAtAsc(String userId);

    @Query(
            "SELECT u FROM UserVideoEntity u WHERE u.desubtitleOutputExpiresAt IS NOT NULL AND u.desubtitleOutputExpiresAt < :t")
    List<UserVideoEntity> findOutputExpiredBefore(@Param("t") Instant t);
}
