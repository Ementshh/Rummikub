package com.rummikub.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "game_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "user_id"}),
        @UniqueConstraint(columnNames = {"game_id", "turn_order"})
})
public class GameParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @ToString.Exclude
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "turn_order", nullable = false)
    private short turnOrder;

    @Column(nullable = false)
    private int score = 0;

    @Column(name = "has_done_initial_meld", nullable = false)
    private boolean hasDoneInitialMeld = false;
}
