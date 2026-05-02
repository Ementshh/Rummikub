package com.rummikub.backend.repositories;

import com.rummikub.backend.models.GameParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameParticipantRepository extends JpaRepository<GameParticipant, UUID> {
    List<GameParticipant> findByGameIdOrderByTurnOrderAsc(String gameId);
    Optional<GameParticipant> findByGameIdAndUserId(String gameId, UUID userId);
    int countByGameId(String gameId);
}
