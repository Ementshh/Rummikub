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
import com.rummikub.screens.states.MyTurnState;
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
     * Uses bounding box hit testing to determine which set (if any) the tile
     * lands on. If the tile lands inside a set's bounding box, it joins that set.
     * If it lands on the table but outside all bounding boxes, it creates a new set.
     */
    public void attachDropListener(TileActor actor, String sourceArea, int sourceSetIndex) {
        ScrollPane tableScroll = tableRenderer.getTableScroll();

        actor.addListener(new com.badlogic.gdx.scenes.scene2d.EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                if (!(event instanceof TileDropEvent)) return false;
                if (!(callback.getCurrentState() instanceof MyTurnState)) return false;

                TileDropEvent drop = (TileDropEvent) event;
                float dropX = drop.dropX;
                float dropY = drop.dropY;

                // Convert stage coords to tableGroup-local coords for bounding box test
                float localDropX = dropX - tableScroll.getX();
                float localDropY = dropY - tableY;
                // Account for scroll offset
                localDropX += tableScroll.getScrollX();

                int targetSetIndex = tableRenderer.findSetIndexAt(localDropX, localDropY);

                Gdx.app.log("DROP", "Tile " + actor.getTileData().id
                    + " src=" + sourceArea + " dropStage=(" + dropX + "," + dropY + ")"
                    + " localDrop=(" + localDropX + "," + localDropY + ")"
                    + " targetBB=" + targetSetIndex);

                if (dropY >= rackY && dropY < rackY + rackH) {
                    // TABLE→RACK is never allowed: table tiles cannot be returned to the rack
                    if ("TABLE".equals(sourceArea)) {
                        callback.showStatusMessage("Tile dari meja tidak bisa dikembalikan ke rack!");
                        return true;
                    }
                } else if (dropY >= tableY && dropY < tableY + tableH) {
                    // Dropped onto table area
                    if ("RACK".equals(sourceArea)) {
                        if (targetSetIndex >= 0) {
                            // Join existing set
                            PlaceTileCommand cmd = new PlaceTileCommand(
                                    actor.getTileData().id, false,
                                    targetSetIndex, "RUN");
                            commandHistory.execute(cmd);
                        } else {
                            // Create new set
                            PlaceTileCommand cmd = new PlaceTileCommand(
                                    actor.getTileData().id, true,
                                    0, "RUN");
                            commandHistory.execute(cmd);
                        }
                        callback.refreshTileDisplay();
                    } else if ("TABLE".equals(sourceArea)) {
                        if (targetSetIndex >= 0 && targetSetIndex != sourceSetIndex) {
                            MoveWithinTableCommand cmd = new MoveWithinTableCommand(
                                    actor.getTileData().id, sourceSetIndex, targetSetIndex);
                            commandHistory.execute(cmd);
                            callback.refreshTileDisplay();
                        } else if (targetSetIndex == -1) {
                            // Moved to empty space — create new set by returning then placing
                            ReturnTileCommand ret = new ReturnTileCommand(
                                    actor.getTileData().id, sourceSetIndex);
                            commandHistory.execute(ret);
                            PlaceTileCommand place = new PlaceTileCommand(
                                    actor.getTileData().id, true, 0, "RUN");
                            commandHistory.execute(place);
                            callback.refreshTileDisplay();
                        }
                    }
                }
                tableRenderer.setHighlightedSetIndex(-1); // Clear highlight
                return true;
            }
        });
    }

    /**
     * Attaches DragMoveListener to track live tile position during drag
     * and highlight the bounding box of the set being hovered.
     */
    public void attachDragMoveListener(TileActor actor, String sourceArea) {
        ScrollPane tableScroll = tableRenderer.getTableScroll();
        boolean isTableSource = "TABLE".equals(sourceArea);

        actor.setDragMoveListener(new TileActor.DragMoveListener() {
            @Override
            public void onDragMove(TileActor a, float stageX, float stageY) {
                if (isTableSource) callback.onTableTileDragStart();

                if (stageY < tableY || stageY >= tableY + tableH) {
                    tableRenderer.setHighlightedSetIndex(-1);
                    return;
                }
                float localX = stageX - tableScroll.getX() + tableScroll.getScrollX();
                float localY = stageY - tableY;
                tableRenderer.setHighlightedSetIndex(
                    tableRenderer.findSetIndexAt(localX, localY));
            }

            @Override
            public void onDragEnd(TileActor a, float stageX, float stageY) {
                tableRenderer.setHighlightedSetIndex(-1);
                if (isTableSource) callback.onTableTileDragEnd();
            }
        });
    }
}
