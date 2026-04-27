package com.rummikub.state;

import com.rummikub.network.NetworkManager;
import com.rummikub.network.dto.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [SINGLETON] — Single source of truth for all local game state.
 *
 * Holds the authoritative local copy of the game data. Screens and actors
 * read from and write to this manager. The snapshot mechanism allows the
 * player to undo all local changes and revert to the state at the start
 * of their turn.
 */
public class GameStateManager {

    private static GameStateManager instance;

    // -------------------------------------------------------------------------
    // Server-synced state
    // -------------------------------------------------------------------------
    private String gameId;
    private String currentTurnUserId;
    private boolean hasDoneInitialMeld;
    private String winnerUsername;

    // Live local state (manipulated by the player during their turn)
    private List<TileDto> myRackTiles = new ArrayList<>();
    private List<TableSetDto> tableSets = new ArrayList<>();
    private List<ParticipantDto> participants = new ArrayList<>();

    // O(1) lookup cache: tile ID → TileDto (populated from myRackTiles on each server sync)
    private Map<Integer, TileDto> tileCache = new HashMap<>();

    // -------------------------------------------------------------------------
    // Snapshot (taken at the start of each turn for undo/reset)
    // -------------------------------------------------------------------------
    private List<TileDto> rackSnapshot = new ArrayList<>();
    private List<TableSetDto> tableSnapshot = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private GameStateManager() {}

    public static GameStateManager getInstance() {
        if (instance == null) {
            instance = new GameStateManager();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Server sync
    // -------------------------------------------------------------------------

    /**
     * Overwrites all local state with fresh data from the server response.
     * Call this after every successful poll.
     */
    public void loadFromServer(GameStateResponse.GameData data) {
        if (data == null) return;

        this.gameId = data.id;
        this.currentTurnUserId = data.currentTurnUserId;
        this.hasDoneInitialMeld = data.hasDoneInitialMeld;
        this.winnerUsername = data.winner;

        this.myRackTiles = data.myRackTiles != null ? new ArrayList<>(data.myRackTiles) : new ArrayList<>();
        this.tableSets = data.tableSets != null ? new ArrayList<>(data.tableSets) : new ArrayList<>();
        this.participants = data.participants != null ? new ArrayList<>(data.participants) : new ArrayList<>();

        // Rebuild tile cache for O(1) lookup by ID
        tileCache.clear();
        for (TileDto t : this.myRackTiles) {
            tileCache.put(t.id, t);
        }
    }

    // -------------------------------------------------------------------------
    // Snapshot management
    // -------------------------------------------------------------------------

    /**
     * Deep-copies the current rack and table state into the snapshot.
     * Call this at the start of the player's turn (MyTurnState.enter).
     */
    public void takeSnapshot() {
        rackSnapshot = deepCopyRack(myRackTiles);
        tableSnapshot = deepCopyTable(tableSets);
    }

    /**
     * Restores rack and table state from the snapshot taken at turn start.
     * Call this when the player clicks RESET.
     */
    public void resetToSnapshot() {
        myRackTiles = deepCopyRack(rackSnapshot);
        tableSets = deepCopyTable(tableSnapshot);
    }

    // -------------------------------------------------------------------------
    // Turn logic
    // -------------------------------------------------------------------------

    /**
     * Returns true if it is currently this client's turn.
     */
    public boolean isMyTurn() {
        String myId = NetworkManager.getInstance().getUserId();
        return currentTurnUserId != null && currentTurnUserId.equals(myId);
    }

    /**
     * Builds the EndTurnRequest payload from the current local state.
     * This is what gets sent to POST /api/games/{id}/end-turn.
     */
    public EndTurnRequest buildEndTurnRequest() {
        List<Integer> rackIds = new ArrayList<>();
        for (TileDto tile : myRackTiles) {
            rackIds.add(tile.id);
        }

        List<TableSetDto> sets = new ArrayList<>();
        for (TableSetDto src : tableSets) {
            TableSetDto copy = new TableSetDto(src.setType, new ArrayList<>(src.tileIds));
            sets.add(copy);
        }

        return new EndTurnRequest(sets, rackIds);
    }

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getCurrentTurnUserId() { return currentTurnUserId; }
    public void setCurrentTurnUserId(String id) { this.currentTurnUserId = id; }

    public boolean isHasDoneInitialMeld() { return hasDoneInitialMeld; }
    public void setHasDoneInitialMeld(boolean hasDoneInitialMeld) { this.hasDoneInitialMeld = hasDoneInitialMeld; }

    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }

    public List<TileDto> getMyRackTiles() { return myRackTiles; }
    public void setMyRackTiles(List<TileDto> tiles) { this.myRackTiles = tiles; }

    public List<TableSetDto> getTableSets() { return tableSets; }
    public void setTableSets(List<TableSetDto> sets) { this.tableSets = sets; }

    public List<ParticipantDto> getParticipants() { return participants; }
    public void setParticipants(List<ParticipantDto> participants) { this.participants = participants; }

    /**
     * O(1) lookup of a TileDto by its ID from the tile cache.
     * The cache is populated from myRackTiles on each call to loadFromServer().
     *
     * @return the TileDto for the given id, or null if not found in cache
     */
    public TileDto getTileById(int id) {
        return tileCache.get(id);
    }

    // -------------------------------------------------------------------------
    // Deep copy helpers
    // -------------------------------------------------------------------------

    private List<TileDto> deepCopyRack(List<TileDto> source) {
        List<TileDto> copy = new ArrayList<>();
        for (TileDto t : source) {
            copy.add(new TileDto(t.id, t.color, t.number, t.isJoker));
        }
        return copy;
    }

    private List<TableSetDto> deepCopyTable(List<TableSetDto> source) {
        List<TableSetDto> copy = new ArrayList<>();
        for (TableSetDto s : source) {
            copy.add(new TableSetDto(s.setType, new ArrayList<>(s.tileIds)));
        }
        return copy;
    }
}
