package com.rummikub.backend.models;

import com.rummikub.backend.models.enums.GameStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "games")
public class Game {

    @Id
    @Column(length = 6)
    private String id;

    @Column(name = "status", nullable = false)
    private String statusStr = GameStatus.WAITING.name();

    public GameStatus getStatus() {
        if (this.statusStr == null) return null;
        return GameStatus.valueOf(this.statusStr);
    }

    public void setStatus(GameStatus status) {
        this.statusStr = (status == null) ? null : status.name();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_turn_participant_id")
    @ToString.Exclude // Prevent circular reference
    private GameParticipant currentTurnParticipant;

    @Column(name = "turn_started_at")
    private LocalDateTime turnStartedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
