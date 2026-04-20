package com.rummikub.network.dto;

import java.util.ArrayList;
import java.util.List;

public class TableSetDto {
    public String setType;          // "RUN" or "GROUP"
    public List<Integer> tileIds;

    public TableSetDto() {
        this.tileIds = new ArrayList<>();
    }

    public TableSetDto(String setType, List<Integer> tileIds) {
        this.setType = setType;
        this.tileIds = tileIds != null ? tileIds : new ArrayList<>();
    }
}
