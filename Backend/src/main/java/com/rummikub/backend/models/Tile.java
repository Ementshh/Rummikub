package com.rummikub.backend.models;

import com.rummikub.backend.models.enums.TileColor;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tiles")
public class Tile {

    @Id
    private Integer id;

    @Column(name = "color")
    private String colorStr;

    public TileColor getColor() {
        if (this.colorStr == null) return null;
        return TileColor.valueOf(this.colorStr);
    }

    public void setColor(TileColor color) {
        this.colorStr = (color == null) ? null : color.name();
    }

    private Integer number;

    @Column(name = "is_joker", nullable = false)
    private boolean isJoker = false;
}
