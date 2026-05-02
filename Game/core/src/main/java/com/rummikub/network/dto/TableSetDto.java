package com.rummikub.network.dto;

import java.util.ArrayList;
import java.util.List;

public class TableSetDto {
    public String set_type;          // "RUN" or "GROUP"
    public List<Integer> tile_ids;
    
    public List<TileDto> tiles;
    public transient boolean isNewThisTurn = false; // true = dibuat pemain saat ini

    public TableSetDto() {
        this.tile_ids = new ArrayList<>();
    }

    public TableSetDto(String set_type, List<Integer> tile_ids) {
        this.set_type = set_type;
        this.tile_ids = tile_ids != null ? tile_ids : new ArrayList<>();
    }
}
