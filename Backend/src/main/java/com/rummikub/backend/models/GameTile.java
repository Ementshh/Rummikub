package com.rummikub.backend.models;

import com.rummikub.backend.models.enums.TileLocation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "game_tiles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "tile_id"})
})
public class GameTile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @ToString.Exclude
    private Game game;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tile_id", nullable = false)
    private Tile tile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "tile_location default 'POOL'")
    private TileLocation location = TileLocation.POOL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    @ToString.Exclude
    private GameParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_set_id")
    @ToString.Exclude
    private TableSet tableSet;
}
