package com.rummikub.backend.models;

import com.rummikub.backend.models.enums.TileColor;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tiles")
public class Tile {

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TileColor color;

    private Integer number;

    @Column(name = "is_joker", nullable = false)
    private boolean isJoker = false;
}
