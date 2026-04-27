package com.rummikub.network.dto;

import java.util.ArrayList;
import java.util.List;

public class TableSetDto {
    public String set_type;          // "RUN" or "GROUP"
    public List<Integer> tileIds;

    public TableSetDto() {
        this.tileIds = new ArrayList<>();
    }

    public TableSetDto(String set_type, List<Integer> tileIds) {
        this.set_type = set_type;
        this.tileIds = tileIds != null ? tileIds : new ArrayList<>();
    }
}
