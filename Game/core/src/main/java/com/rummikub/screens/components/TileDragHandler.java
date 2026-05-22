package com.rummikub.screens.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.rummikub.actors.TileActor;
import com.rummikub.actors.TileDropEvent;
import com.rummikub.command.CommandHistory;
import com.rummikub.command.MoveWithinTableCommand;
import com.rummikub.command.PlaceTileCommand;
import com.rummikub.command.ReturnTileCommand;
import com.rummikub.network.dto.TableSetDto;
import com.rummikub.screens.GameScreenState;
import com.rummikub.state.GameStateManager;

/**
 * Handles all tile drag-and-drop logic, including command execution.
 * Uses a {@link Callback} interface to communicate back to GameScreen
 * without creating a direct dependency on the screen class.
 */
public class TileDragHandler {

    /**
     * Callback interface so the handler can trigger screen-level actions
     * without depending on GameScreen directly (Dependency Inversion).
     */
    public interface Callback {
        void refreshTileDisplay();
        void showStatusMessage(String msg);
        GameScreenState getCurrentState();
        void onTableTileDragStart();
        void onTableTileDragEnd();
    }

    // Layout zone boundaries
    private final float rackY;
    private final float rackH;
    private final float tableY;
    private final float tableH;

    private final GameStateManager gsm;
    private final CommandHistory commandHistory;
    private final TableRenderer tableRenderer;
    private final Callback callback;

    public TileDragHandler(float rackY, float rackH, float tableY, float tableH,
                           GameStateManager gsm, CommandHistory commandHistory,
                           TableRenderer tableRenderer, Callback callback) {
        this.rackY = rackY;
        this.rackH = rackH;
        this.tableY = tableY;
        this.tableH = tableH;
        this.gsm = gsm;
        this.commandHistory = commandHistory;
        this.tableRenderer = tableRenderer;
        this.callback = callback;
    }

    /**
     * Attaches a TileDropEvent listener to a TileActor that executes the
     * appropriate Command when the tile is dropped.
     *
     * @param isCommittedSet true if the tile belongs to a set that was
     *                       committed in a previous end-turn (isNewThisTurn=false).
     *                       Such tiles cannot be returned to the rack.
     */
    public void attachDropListener(TileActor actor, String sourceArea, int sourceSetIndex, boolean isCommittedSet) {
        ScrollPane tableScroll = tableRenderer.getTableScroll();

        actor.addListener(new com.badlogic.gdx.scenes.scene2d.EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                if (!(event instanceof TileDropEvent)) return false;

                GameScreenState state = callback.getCurrentState();
                if (state == null) return false;

                TileDropEvent drop = (TileDropEvent) event;
                float dropX = drop.dropX;
                float dropY = drop.dropY;

                // Convert stage coords to tableGroup-local coords for bounding box test
                float localDropX = dropX - tableScroll.getX();
                float localDropY = dropY - tableY;
                // Account for scroll offset
                localDropX += tableScroll.getScrollX();

                int targetSetIndex = tableRenderer.findSetIndexAt(localDropX, localDropY);
                int targetSlotIndex = tableRenderer.findEmptySlotIndexAt(localDropX, localDropY);

                Gdx.app.log("DROP", "Tile " + actor.getTileData().id
                    + " src=" + sourceArea + " dropStage=(" + dropX + "," + dropY + ")"
                    + " localDrop=(" + localDropX + "," + localDropY + ")"
                    + " targetSet=" + targetSetIndex + " targetSlot=" + targetSlotIndex);

                if (dropY >= rackY && dropY < rackY + rackH) {
                    // Dropped onto rack area
                    if ("TABLE".equals(sourceArea)) {
                        // Table→Rack: requires table interaction permission
                        if (!state.canInteractWithTable()) return false;
                        if (isCommittedSet) {
                            callback.showStatusMessage("Set lama tidak bisa dikembalikan ke rack!");
                        } else {
                            ReturnTileCommand cmd = new ReturnTileCommand(
                                    actor.getTileData().id, sourceSetIndex);
                            commandHistory.execute(cmd);
                            com.rummikub.utils.ResourcePool.getInstance().getPlaceSound().play();
                            callback.refreshTileDisplay();
                        }
                        return true;
                    }
                    if ("RACK".equals(sourceArea) && state.canInteractWithRack()) {
                        // Rack ke Rack: reorder tiles dalam rack
                        reorderRackTile(actor, dropX);
                        com.rummikub.utils.ResourcePool.getInstance().getPlaceSound().play();
                        callback.refreshTileDisplay();
                        return true;
                    }
                } else if (dropY >= tableY && dropY < tableY + tableH) {
                    // Drop ke table, butuh permission table interaction
                    if (!state.canInteractWithTable()) return false;

                    if ("RACK".equals(sourceArea)) {
                        if (targetSetIndex >= 0) {
                            // Joining an existing set — check initial meld rule
                            java.util.List<com.rummikub.network.dto.TableSetDto> sets = gsm.getTableSets();
                            boolean isTargetCommitted = targetSetIndex < sets.size()
                                    && !sets.get(targetSetIndex).isNewThisTurn;

                            if (!gsm.isHasDoneInitialMeld() && isTargetCommitted) {
                                callback.showStatusMessage(
                                        "Belum bisa manipulasi set lama — selesaikan initial meld (≥30 poin) dulu!");
                                return true;
                            }

                            PlaceTileCommand cmd = new PlaceTileCommand(
                                    actor.getTileData().id, false,
                                    targetSetIndex, "RUN");
                            commandHistory.execute(cmd);
                        } else if (targetSlotIndex >= 0) {
                            // Create new set at specific grid position
                            PlaceTileCommand cmd = new PlaceTileCommand(
                                    actor.getTileData().id, true,
                                    targetSlotIndex, "RUN");
                            commandHistory.execute(cmd);
                        } else {
                            // Create new set at the end
                            PlaceTileCommand cmd = new PlaceTileCommand(
                                    actor.getTileData().id, true,
                                    -1, "RUN");
                            commandHistory.execute(cmd);
                        }
                        com.rummikub.utils.ResourcePool.getInstance().getPlaceSound().play();
                        callback.refreshTileDisplay();
                    } else if ("TABLE".equals(sourceArea)) {
                        if (targetSetIndex >= 0 && targetSetIndex != sourceSetIndex) {
                            MoveWithinTableCommand cmd = new MoveWithinTableCommand(
                                    actor.getTileData().id, sourceSetIndex, targetSetIndex);
                            commandHistory.execute(cmd);
                            com.rummikub.utils.ResourcePool.getInstance().getPlaceSound().play();
                            callback.refreshTileDisplay();
                        } else if (targetSlotIndex >= 0) {
                            // Moved to empty slot — create new set at specific position
                            ReturnTileCommand ret = new ReturnTileCommand(
                                    actor.getTileData().id, sourceSetIndex);
                            commandHistory.execute(ret);
                            PlaceTileCommand place = new PlaceTileCommand(
                                    actor.getTileData().id, true, targetSlotIndex, "RUN");
                            commandHistory.execute(place);
                            com.rummikub.utils.ResourcePool.getInstance().getPlaceSound().play();
                            callback.refreshTileDisplay();
                        } else if (targetSetIndex == -1 && targetSlotIndex == -1) {
                            // Moved to empty space outside any slot — create new set at end
                            ReturnTileCommand ret = new ReturnTileCommand(
                                    actor.getTileData().id, sourceSetIndex);
                            commandHistory.execute(ret);
                            PlaceTileCommand place = new PlaceTileCommand(
                                    actor.getTileData().id, true, -1, "RUN");
                            commandHistory.execute(place);
                            com.rummikub.utils.ResourcePool.getInstance().getPlaceSound().play();
                            callback.refreshTileDisplay();
                        }
                    }
                }
                tableRenderer.setHighlightedSetIndex(-1); // Clear highlight
                tableRenderer.setHighlightedSlotIndex(-1);
                return true;
            }
        });
    }


    // Reorder tile dalam rack berdasarkan dia di drop horizontally
    private void reorderRackTile(TileActor actor, float dropX) {
        int tileId = actor.getTileData().id;
        java.util.List<com.rummikub.network.dto.TileDto> rack = gsm.getMyRackTiles();

        // Find current index
        int currentIndex = -1;
        for (int i = 0; i < rack.size(); i++) {
            if (rack.get(i).id == tileId) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1) return;

        // Calculate target index dari drop position
        float tileW = com.rummikub.utils.Constants.TILE_WIDTH + 4f;
        float startX = 10f;
        int targetIndex = (int) ((dropX - startX + tileW / 2f) / tileW);
        targetIndex = Math.max(0, Math.min(targetIndex, rack.size() - 1));

        if (targetIndex == currentIndex) return;

        // Reorder: remove from current position and insert at target
        com.rummikub.network.dto.TileDto tile = rack.remove(currentIndex);
        rack.add(targetIndex, tile);
    }

    /**
     * Attaches DragMoveListener to track live tile position during drag
     * and highlight the bounding box of the set being hovered.
     *
     * @param isCommittedSet true if the source set was committed in a previous end-turn.
     *                       Only committed tiles trigger the rack forbidden overlay.
     */
    public void attachDragMoveListener(TileActor actor, String sourceArea, boolean isCommittedSet) {
        ScrollPane tableScroll = tableRenderer.getTableScroll();
        boolean isTableSource = "TABLE".equals(sourceArea);

        actor.setDragMoveListener(new TileActor.DragMoveListener() {
            @Override
            public void onDragMove(TileActor a, float stageX, float stageY) {
                // Only show forbidden overlay when dragging a committed table tile
                if (isTableSource && isCommittedSet) callback.onTableTileDragStart();

                if (stageY < tableY || stageY >= tableY + tableH) {
                    tableRenderer.setHighlightedSetIndex(-1);
                    tableRenderer.setHighlightedSlotIndex(-1);
                    return;
                }
                float localX = stageX - tableScroll.getX() + tableScroll.getScrollX();
                float localY = stageY - tableY;

                int setIndex = tableRenderer.findSetIndexAt(localX, localY);
                int slotIndex = tableRenderer.findEmptySlotIndexAt(localX, localY);

                tableRenderer.setHighlightedSetIndex(setIndex);
                tableRenderer.setHighlightedSlotIndex(slotIndex);
            }

            @Override
            public void onDragEnd(TileActor a, float stageX, float stageY) {
                tableRenderer.setHighlightedSetIndex(-1);
                tableRenderer.setHighlightedSlotIndex(-1);
                if (isTableSource && isCommittedSet) callback.onTableTileDragEnd();
            }
        });
    }
}
