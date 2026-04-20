package com.rummikub.network.dto;

import java.util.List;

public class EndTurnRequest {
    public List<TableSetDto> table_sets;
    public List<Integer> rack_tiles;

    public EndTurnRequest() {}

    public EndTurnRequest(List<TableSetDto> tableSets, List<Integer> rackTiles) {
        this.table_sets = tableSets;
        this.rack_tiles = rackTiles;
    }
}
