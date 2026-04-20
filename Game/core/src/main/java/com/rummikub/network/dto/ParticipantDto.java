package com.rummikub.network.dto;

public class ParticipantDto {
    public String userId;
    public String username;
    public int turnOrder;
    public boolean hasDoneInitialMeld;

    public ParticipantDto() {}

    public ParticipantDto(String userId, String username, int turnOrder, boolean hasDoneInitialMeld) {
        this.userId = userId;
        this.username = username;
        this.turnOrder = turnOrder;
        this.hasDoneInitialMeld = hasDoneInitialMeld;
    }
}
