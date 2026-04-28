package com.rummikub.state;

import com.rummikub.network.NetworkManager;
import com.rummikub.network.dto.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private long turnStartedAt;
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
        this.turnStartedAt = (data.turnStartedAt != null) ? data.turnStartedAt : 0L;

        // Rebuild tile cache for O(1) lookup by ID
        if (tileCache == null) tileCache = new HashMap<>();
        tileCache.clear();
        
        // Cache rack tiles
        for (TileDto t : this.myRackTiles) {
            tileCache.put(t.id, t);
        }
        
        // Cache table tiles (if server provided them in TableSetDto.tiles)
        for (TableSetDto set : this.tableSets) {
            if (set.tiles != null) {
                for (TileDto t : set.tiles) {
                    tileCache.put(t.id, t);
                }
            }
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
        if (myId == null || currentTurnUserId == null) return false;
        return currentTurnUserId.trim().equalsIgnoreCase(myId.trim());
    }

    public String detectSetType(List<Integer> tile_ids) {
        if (tile_ids == null || tile_ids.size() < 2) return "RUN";

        // Kumpulkan tile non-joker saja untuk pengecekan
        List<TileDto> nonJokers = tile_ids.stream()
            .map(id -> getTileById(id))
            .filter(t -> t != null && !t.isJoker)
            .collect(Collectors.toList());

        // Jika semua tile adalah joker, default ke RUN
        if (nonJokers.isEmpty()) return "RUN";

        // Cek GROUP: semua angka sama?
        int firstNumber = nonJokers.get(0).number;
        boolean allSameNumber = nonJokers.stream().allMatch(t -> t.number == firstNumber);
        if (allSameNumber) return "GROUP";

        // Cek RUN: semua warna sama?
        String firstColor = nonJokers.get(0).color;
        boolean allSameColor = nonJokers.stream().allMatch(t -> t.color != null && t.color.equals(firstColor));
        if (allSameColor) return "RUN";

        // Campuran angka berbeda dan warna berbeda — tidak valid, tapi biarkan server yang tolak
        return "RUN";
    }

    /**
     * Builds the EndTurnRequest payload from the current local state.
     * This is what gets sent to POST /api/games/{id}/end-turn.
     */
    public EndTurnRequest buildEndTurnRequest() {
        EndTurnRequest req = new EndTurnRequest();
        req.table_sets = new ArrayList<>();
        
        for (TableSetDto set : tableSets) {
            // Buat objek baru yang bersih untuk dikirim ke server
            TableSetDto payload = new TableSetDto();
            payload.set_type = detectSetType(set.tile_ids); // Auto-detect before sending
            payload.tile_ids = new ArrayList<>(set.tile_ids);
            
            // Validasi: jangan kirim set kosong
            if (payload.tile_ids.isEmpty()) continue;
            
            // Auto-sort tile_ids based on set type so backend validates correct order
            sortTileIds(payload);
            
            req.table_sets.add(payload);
            
            com.badlogic.gdx.Gdx.app.log("END_TURN_REQ", 
                "set_type=" + payload.set_type + " tile_ids=" + payload.tile_ids);
        }
        
        req.rack_tiles = new ArrayList<>();
        for (TileDto tile : myRackTiles) {
            req.rack_tiles.add(tile.id);
        }

        com.badlogic.gdx.Gdx.app.log("END_TURN_REQ", "rack_tiles=" + req.rack_tiles);
        return req;
    }

    /**
     * Sorts tile_ids in the payload based on the set type:
     * - RUN: sort by tile number (ascending) for sequential validation
     * - GROUP: sort by color name for consistency
     * Jokers are placed at the end.
     */
    private void sortTileIds(TableSetDto payload) {
        if (payload.tile_ids == null || payload.tile_ids.size() < 2) return;
        
        payload.tile_ids.sort((idA, idB) -> {
            TileDto a = getTileById(idA);
            TileDto b = getTileById(idB);
            if (a == null || b == null) return 0;
            
            // Jokers go to the end
            if (a.isJoker && !b.isJoker) return 1;
            if (!a.isJoker && b.isJoker) return -1;
            if (a.isJoker && b.isJoker) return 0;
            
            if ("RUN".equals(payload.set_type)) {
                return Integer.compare(a.number, b.number);
            } else {
                // GROUP: sort by color for consistency
                return (a.color != null && b.color != null) 
                    ? a.color.compareTo(b.color) 
                    : 0;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getCurrentTurnUserId() { return currentTurnUserId; }

    public int getRemainingSeconds() {
        if (turnStartedAt <= 0) return 120;
        long elapsed = (System.currentTimeMillis() - turnStartedAt) / 1000;
        return (int) Math.max(0, 120 - elapsed);
    }
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
        if (source == null) return new ArrayList<>();
        List<TileDto> copy = new ArrayList<>();
        for (TileDto t : source) {
            copy.add(new TileDto(t.id, t.color, t.number, t.isJoker));
        }
        return copy;
    }

    private List<TableSetDto> deepCopyTable(List<TableSetDto> source) {
        if (source == null) return new ArrayList<>();
        List<TableSetDto> copy = new ArrayList<>();
        for (TableSetDto s : source) {
            TableSetDto newSet = new TableSetDto(s.set_type, new ArrayList<>(s.tile_ids));
            // Also copy tiles cache list if present
            if (s.tiles != null) {
                newSet.tiles = new ArrayList<>(s.tiles);
            }
            copy.add(newSet);
        }
        return copy;
    }
}
