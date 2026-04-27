package com.rummikub.network.dto;

import java.util.List;

public class GameStateResponse {
    public boolean success;
    public GameData data;
    public String error;

    public GameStateResponse() {}

    public static class GameData {
        public String id;                           // game UUID
        public String status;                       // WAITING / IN_PROGRESS / FINISHED
        public String currentTurnUserId;
        public List<TileDto> myRackTiles;
        public List<TableSetDto> tableSets;
        public List<ParticipantDto> participants;
        public String winner;                       // username pemenang, null jika belum
        public boolean hasDoneInitialMeld;
        public Long turnStartedAt;                  // Unix timestamp millis (null jika game belum mulai)

        public GameData() {}
    }
}
