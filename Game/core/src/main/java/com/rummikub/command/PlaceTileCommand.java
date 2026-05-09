package com.rummikub.command;

import com.rummikub.network.dto.TableSetDto;
import com.rummikub.network.dto.TileDto;
import com.rummikub.state.GameStateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves a tile from the player's rack to the table.
 *
 * If {@code toNewSet} is true a brand-new {@link TableSetDto} is created;
 * otherwise the tile is appended to the existing set at {@code targetSetIndex}.
 *
 * When creating a new set, if targetSetIndex >= 0, the set is placed at that
 * exact grid position. Gaps (empty sets) are created as placeholders if needed.
 * If targetSetIndex == -1, the set is appended at the end.
 */
public class PlaceTileCommand implements TileCommand {

    private final int tileId;
    private final boolean toNewSet;
    private final int targetSetIndex;  
    private final String setType;

    private final GameStateManager gsm = GameStateManager.getInstance();

    private TileDto savedTile;

    private int originalSetsSize = -1;
    private int gapsCreated = 0;

    public PlaceTileCommand(int tileId, boolean toNewSet, int targetSetIndex, String setType) {
        this.tileId         = tileId;
        this.toNewSet       = toNewSet;
        this.targetSetIndex = targetSetIndex;
        this.setType        = setType;
    }

    @Override
    public void execute() {
        List<TileDto> rack = gsm.getMyRackTiles();


        // Tambahkan dan remove tile dari rack
        TileDto found = null;
        for (TileDto t : rack) {
            if (t.id == tileId) {
                found = t;
                break;
            }
        }
        if (found == null) return;

        savedTile = found;
        rack.remove(found);

        List<TableSetDto> sets = gsm.getTableSets();

        if (toNewSet) {
            TableSetDto newSet = new TableSetDto(setType, new ArrayList<>());
            newSet.isNewThisTurn = true; // set baru = boleh di-detect
            newSet.tile_ids.add(tileId);
            newSet.set_type = gsm.detectSetType(newSet.tile_ids);

            // Trach size awal untuk undo
            originalSetsSize = sets.size();


            // Jika targetSetIndex >= 0, set akan ditempatkan di posisi grid tersebut
            // Jika targetSetIndex == -1, set akan ditambahkan di akhir
            int targetSlot = targetSetIndex >= 0 ? targetSetIndex : sets.size();

            // Isi gap jika diperlukan (buat set kosong sebagai placeholder)
            gapsCreated = 0;
            while (sets.size() < targetSlot) {
                sets.add(TableSetDto.createEmpty());
                gapsCreated++;
            }

            // Sekarang tambahkan set baru di posisi target
            if (targetSlot < sets.size()) {
                // Gantikan set kosong yang ada di slot ini, atau geser
                TableSetDto existing = sets.get(targetSlot);
                if (existing != null && existing.isEmpty()) {
                    sets.set(targetSlot, newSet); // Gantikan placeholder kosong
                } else {
                    sets.add(targetSlot, newSet); // Insert and shift
                }
            } else {
                sets.add(newSet);
            }
        } else {
            TableSetDto target = sets.get(targetSetIndex);
            target.tile_ids.add(tileId);
            // Hanya update set_type untuk set yang BARU dibuat pemain ini
            if (target.isNewThisTurn) {
                target.set_type = gsm.detectSetType(target.tile_ids);
            }
        }
    }

    @Override
    public void undo() {
        if (savedTile == null) return;

        List<TableSetDto> sets = gsm.getTableSets();

        if (toNewSet) {
            // Cari dan hapus set dengan tile kita
            int setIndexToRemove = -1;
            for (int i = 0; i < sets.size(); i++) {
                TableSetDto s = sets.get(i);
                if (s.tile_ids.size() == 1 && s.tile_ids.get(0) == tileId) {
                    setIndexToRemove = i;
                    break;
                }
            }

            if (setIndexToRemove >= 0) {
                sets.remove(setIndexToRemove);
            }

            // Hapus gap yang kita buat, mengembalikan ukuran list asli
            int currentSize = sets.size();
            int targetSize = originalSetsSize;
            for (int i = currentSize - 1; i >= targetSize && i >= 0; i--) {
                TableSetDto s = sets.get(i);
                if (s != null && s.isEmpty()) {
                    sets.remove(i);
                }
            }
        } else {
            TableSetDto target = sets.get(targetSetIndex);
            target.tile_ids.remove(Integer.valueOf(tileId));
            if (target.tile_ids.isEmpty()) {
                sets.remove(targetSetIndex);
            } else {
                if (target.isNewThisTurn) {
                    target.set_type = gsm.detectSetType(target.tile_ids);
                }
            }
        }

        gsm.getMyRackTiles().add(savedTile);
        savedTile = null;
    }
}
