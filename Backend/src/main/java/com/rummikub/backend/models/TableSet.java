package com.rummikub.backend.models;

import com.rummikub.backend.models.enums.SetType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "table_sets")
public class TableSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @ToString.Exclude
    private Game game;

    @Column(name = "set_type", nullable = false)
    private String setTypeStr;

    public SetType getSetType() {
        if (this.setTypeStr == null) return null;
        return SetType.valueOf(this.setTypeStr);
    }

    public void setSetType(SetType setType) {
        this.setTypeStr = (setType == null) ? null : setType.name();
    }
}
