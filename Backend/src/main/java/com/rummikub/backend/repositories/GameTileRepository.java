package com.rummikub.backend.repositories;

import com.rummikub.backend.models.GameTile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameTileRepository extends JpaRepository<GameTile, UUID> {
    List<GameTile> findByGameId(String gameId);
    List<GameTile> findByGameIdAndLocationStr(String gameId, String locationStr);
    
    List<GameTile> findByGameIdAndParticipantIdAndLocationStr(String gameId, UUID participantId, String locationStr);
    
    @Query("SELECT gt FROM GameTile gt WHERE gt.game.id = :gameId AND (gt.locationStr = 'TABLE' OR (gt.locationStr = 'RACK' AND gt.participant.id = :participantId))")
    List<GameTile> findBoardAndRackState(@Param("gameId") String gameId, @Param("participantId") UUID participantId);

    void deleteByGameId(String gameId);
    
    GameTile findByGameIdAndTileId(String gameId, Integer tileId);
}
