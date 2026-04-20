package com.rummikub.network.dto;

public class TileDto {
    public int id;
    public String color;    // "RED", "BLUE", "YELLOW", "BLACK"
    public int number;      // 1–13
    public boolean isJoker;

    public TileDto() {}

    public TileDto(int id, String color, int number, boolean isJoker) {
        this.id = id;
        this.color = color;
        this.number = number;
        this.isJoker = isJoker;
    }
}
