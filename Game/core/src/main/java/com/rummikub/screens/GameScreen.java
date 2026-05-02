package com.rummikub.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.rummikub.RummikubGame;
import com.rummikub.actors.TileActor;
import com.rummikub.actors.TileDropEvent;
import com.rummikub.command.CommandHistory;
import com.rummikub.command.MoveWithinTableCommand;
import com.rummikub.command.PlaceTileCommand;
import com.rummikub.command.ReturnTileCommand;
import com.rummikub.factory.TileActorFactory;
import com.rummikub.network.ApiCallback;
import com.rummikub.network.GameApiFacade;
import com.rummikub.network.NetworkManager;
import com.rummikub.network.dto.*;
import com.rummikub.screens.states.*;
import com.rummikub.state.GameStateManager;
import com.rummikub.strategy.LockedTileStrategy;
import com.rummikub.strategy.RackTileStrategy;
import com.rummikub.strategy.TableTileStrategy;
import com.rummikub.strategy.TileRenderStrategy;
import com.rummikub.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;

/**
 * Main game screen.
 *

 *
 * Integrates the State Machine (WaitingTurnState / MyTurnState /
 * SubmittingState / GameOverState) and the Command pattern for undo.
 */
public class GameScreen extends BaseScreen {

    // -------------------------------------------------------------------------
    // Constants (layout)
    // -------------------------------------------------------------------------
    private static final float HEADER_H     = 60f;
    private static final float BUTTON_BAR_H = 60f;
    private static final float RACK_H       = 100f;
    private static final float TABLE_H      = Constants.SCREEN_HEIGHT - HEADER_H - BUTTON_BAR_H - RACK_H;

    private static final float RACK_Y       = 0f;
    private static final float BTN_BAR_Y    = RACK_H;
    private static final float TABLE_Y      = RACK_H + BUTTON_BAR_H;
    private static final float HEADER_Y     = TABLE_Y + TABLE_H;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    private final String gameId;
    private final GameApiFacade facade;
    private final GameStateManager gsm;
    private final CommandHistory commandHistory;

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------
    private GameScreenState currentState;

    // -------------------------------------------------------------------------
    // UI components
    // -------------------------------------------------------------------------
    private Label timerLabel;
    private Label turnInfoLabel;
    private Label statusLabel;
    private Label waitingOverlay;

    private TextButton drawButton;
    private TextButton resetButton;
    private TextButton endTurnButton;

    /** Container for rack tile actors. */
    private Group rackGroup;
    /** Container for table set groups. */
    private Group tableGroup;
    /** Horizontal scroll pane wrapping tableGroup. */
    private ScrollPane tableScroll;

    /** Label showing the opponent's name in the left side of the header. */
    private Label opponentNameLabel;

    /** Shared font for tile rendering (owned by this screen). */
    private BitmapFont tileFont;

    // -------------------------------------------------------------------------
    // Bounding box highlight system
    // -------------------------------------------------------------------------

    /** ShapeRenderer for drawing bounding box highlights. */
    private ShapeRenderer bbRenderer;

    /** Index of set currently being hovered during drag. -1 = none. */
    private int highlightedSetIndex = -1;

    /** Cached bounding boxes for each set (in tableGroup local coords). */
    private final List<Rectangle> setBoundingBoxes = new ArrayList<>();

    /** Padding around tiles to form the bounding box. */
    private static final float BB_PADDING = 10f;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public GameScreen(RummikubGame game, String gameId) {
        super(game);
        this.gameId = gameId;
        this.facade = new GameApiFacade();
        this.gsm = GameStateManager.getInstance();
        this.commandHistory = new CommandHistory();
    }

    // -------------------------------------------------------------------------
    // buildUI — Template Method hook
    // -------------------------------------------------------------------------

    @Override
    protected void buildUI() {
        tileFont = new BitmapFont();
        bbRenderer = new ShapeRenderer();

        buildHeader();
        buildTableArea();
        buildButtonBar();
        buildRackArea();
        buildWaitingOverlay();

        // Fetch latest state from server before determining initial state
        pollGameState();

        // Initial state: determine whose turn it is
        if (gsm.isMyTurn()) {
            transitionTo(new MyTurnState());
        } else {
            transitionTo(new WaitingTurnState());
        }

        refreshTileDisplay();
    }

    // -------------------------------------------------------------------------
    // Layout builders
    // -------------------------------------------------------------------------

    private void buildHeader() {
        Table header = new Table();
        header.setBounds(0, HEADER_Y, Constants.SCREEN_WIDTH, HEADER_H);
        header.setBackground(makeColorDrawable(new Color(0.08f, 0.14f, 0.08f, 1f)));
        header.left().pad(10);

        // Resolve opponent name from participants at build time (may be empty until first poll)
        String opponentName = "";
        String localUser = NetworkManager.getInstance().getCurrentUsername();
        for (ParticipantDto p : gsm.getParticipants()) {
            if (localUser == null || !localUser.equals(p.username)) {
                opponentName = p.username;
                break;
            }
        }

        opponentNameLabel = makeLabel(opponentName);
        turnInfoLabel  = makeLabel("Giliran: ...");
        timerLabel = makeLabel("TIMER: --:--");
        timerLabel.setColor(Color.YELLOW);

        header.add(opponentNameLabel).expandX().left();
        header.add(turnInfoLabel).expandX().center();
        header.add(timerLabel).right().padRight(20);

        stage.addActor(header);
    }

    private void buildTableArea() {
        // Scrollable horizontal area for table sets
        tableGroup = new Group();
        tableGroup.setSize(3000, TABLE_H - 20); // wide enough for many sets

        tableScroll = new ScrollPane(tableGroup, buildScrollPaneStyle());
        tableScroll.setBounds(0, TABLE_Y, Constants.SCREEN_WIDTH, TABLE_H);
        tableScroll.setScrollingDisabled(false, true);
        tableScroll.setFadeScrollBars(false);
        tableScroll.setCancelTouchFocus(false);

        stage.addActor(tableScroll);
    }

    private void buildButtonBar() {
        Table bar = new Table();
        bar.setBounds(0, BTN_BAR_Y, Constants.SCREEN_WIDTH, BUTTON_BAR_H);
        bar.setBackground(makeColorDrawable(new Color(0.10f, 0.10f, 0.10f, 1f)));
        bar.pad(8);

        drawButton    = makeButton("DRAW",     new Color(0.20f, 0.40f, 0.70f, 1f));
        resetButton   = makeButton("RESET",    new Color(0.50f, 0.35f, 0.10f, 1f));
        endTurnButton = makeButton("END TURN", new Color(0.15f, 0.55f, 0.15f, 1f));
        statusLabel = makeLabel("Meld: BELUM (min 30 poin)");
        statusLabel.setColor(Color.ORANGE);

        // All buttons disabled by default — enabled only when MyTurnState is active
        drawButton.setDisabled(true);
        resetButton.setDisabled(true);
        endTurnButton.setDisabled(true);

        bar.add(drawButton).width(120).height(44).padRight(12);
        bar.add(resetButton).width(120).height(44).padRight(12);
        bar.add(endTurnButton).width(140).height(44).padRight(20);
        bar.add(statusLabel).expandX().left();

        stage.addActor(bar);

        // Listeners
        drawButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onDrawClicked();
            }
        });

        resetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onResetClicked();
            }
        });

        endTurnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onEndTurnClicked();
            }
        });
    }

    private void buildRackArea() {

        // Username label
        String username = NetworkManager.getInstance().getCurrentUsername();
        Label nameLabel = makeLabel(username != null ? username : "Pemain");
        nameLabel.setPosition(10, RACK_Y + RACK_H - 18);
        stage.addActor(nameLabel);

        // Rack Background (Dark Brown)
        Image rackBg = new Image(makeColorDrawable(new Color(0.35f, 0.22f, 0.10f, 1f)));
        rackBg.setBounds(0, 0, Constants.SCREEN_WIDTH, RACK_H + RACK_Y);
        stage.addActor(rackBg);

        // Rack Border (Light Brown)
        Image rackBorder = new Image(makeColorDrawable(new Color(0.55f, 0.38f, 0.18f, 1f)));
        rackBorder.setBounds(0, RACK_Y + RACK_H - 2, Constants.SCREEN_WIDTH, 3);
        stage.addActor(rackBorder);

        // Group for tile actors
        rackGroup = new Group();
        rackGroup.setBounds(0, RACK_Y, Constants.SCREEN_WIDTH, RACK_H);
        stage.addActor(rackGroup);
    }

    private void buildWaitingOverlay() {
        waitingOverlay = makeLabel("Menunggu giliran pemain lain...");
        waitingOverlay.setColor(new Color(1f, 1f, 0.5f, 1f));
        waitingOverlay.setFontScale(1.4f);
        waitingOverlay.setPosition(
                Constants.SCREEN_WIDTH / 2f - 200,
                TABLE_Y + TABLE_H / 2f);
        waitingOverlay.setVisible(false);
        stage.addActor(waitingOverlay);
    }

    // -------------------------------------------------------------------------
    // update — Template Method hook
    // -------------------------------------------------------------------------

    @Override
    protected void update(float delta) {
        if (currentState != null) {
            currentState.update(this, delta);
        }
    }

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------

    /** Transitions to a new state, calling exit() on the old and enter() on the new. */
    public void transitionTo(GameScreenState newState) {
        if (newState == null) {
            Gdx.app.log("GameScreen", "transitionTo: newState is null, ignoring transition");
            return;
        }
        if (currentState != null) {
            currentState.exit(this);
        }
        currentState = newState;
        currentState.enter(this);
    }

    // -------------------------------------------------------------------------
    // State callbacks (called by state objects)
    // -------------------------------------------------------------------------

    /** Enables or disables interactive controls based on whose turn it is. */
    public void setControlsEnabled(boolean enabled) {
        drawButton.setDisabled(!enabled);
        resetButton.setDisabled(!enabled);
        endTurnButton.setDisabled(!enabled);
        waitingOverlay.setVisible(!enabled);

        com.badlogic.gdx.scenes.scene2d.Touchable touchable = enabled
                ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                : com.badlogic.gdx.scenes.scene2d.Touchable.disabled;

        // Toggle drag on rack tiles
        for (Actor a : rackGroup.getChildren()) {
            a.setTouchable(touchable);
        }

        // Toggle drag on table tiles
        for (Actor a : tableGroup.getChildren()) {
            a.setTouchable(touchable);
        }
    }

    /** Polls the server for the latest game state. */
    public void pollGameState() {
        facade.getGameState(gameId, new ApiCallback<GameStateResponse>() {
            @Override
            public void onSuccess(GameStateResponse r) {
                if (r == null || !r.success || r.data == null) return;

                if ("FINISHED".equals(r.data.status)) {
                    gsm.loadFromServer(r.data);
                    transitionTo(new GameOverState());
                    return;
                }

                gsm.loadFromServer(r.data);
                
                // BUG 2 DEBUG
                Gdx.app.log("DEBUG_TURN", "currentTurnUserId dari server: " + r.data.currentTurnUserId);
                Gdx.app.log("DEBUG_TURN", "userId kita (NetworkManager): " + NetworkManager.getInstance().getUserId());
                Gdx.app.log("DEBUG_TURN", "isMyTurn() result: " + gsm.isMyTurn());

                refreshTileDisplay();
                updateMeldPointsDisplay();

                if (gsm.isMyTurn() && !(currentState instanceof MyTurnState)) {
                    transitionTo(new MyTurnState());
                }
            }

            @Override
            public void onFailure(String err) {
                Gdx.app.log("GameScreen", "Poll error: " + err);
            }
        });
    }

    /** Updates the on-screen countdown timer label. */
    public void updateTimerDisplay(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        timerLabel.setText(String.format("TIMER: %02d:%02d", m, s));
        timerLabel.setColor(seconds <= 15 ? Color.RED : Color.YELLOW);
    }

    /** Called when the turn timer reaches zero — auto-draw. */
    public void onTimerExpired() {
        transitionTo(new SubmittingState());
        facade.drawTile(gameId, new ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse r) {
                commandHistory.clear();
                pollGameState();
                transitionTo(new WaitingTurnState());
            }

            @Override
            public void onFailure(String err) {
                Gdx.app.log("GameScreen", "Auto-draw failed: " + err);
                transitionTo(new WaitingTurnState());
            }
        });
    }

    /** Shows the game-over result panel. */
    public void showGameOverPanel() {
        String winner = gsm.getWinnerUsername();
        game.setScreen(new GameOverScreen(game, winner));
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    private void onDrawClicked() {
        transitionTo(new SubmittingState());
        facade.drawTile(gameId, new ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse r) {
                commandHistory.clear();
                if (r.success) {
                    pollGameState();
                    transitionTo(new WaitingTurnState());
                } else {
                    Gdx.app.log("GameScreen", "Draw failed: " + r.error);
                    transitionTo(new MyTurnState());
                }
            }

            @Override
            public void onFailure(String err) {
                Gdx.app.log("GameScreen", "Draw error: " + err);
                transitionTo(new MyTurnState());
            }
        });
    }

    private void onResetClicked() {
        commandHistory.undoAll();
        gsm.resetToSnapshot();
        refreshTileDisplay();
    }

    private void onEndTurnClicked() {
        List<TableSetDto> sets = gsm.getTableSets();

        // Hitung jumlah tile baru di meja (dari set yang isNewThisTurn = true)
        int newTileCount = 0;
        for (TableSetDto set : sets) {
            if (set.isNewThisTurn) {
                newTileCount += set.tile_ids.size();
            }
        }

        // Aturan untuk pemain yang belum initial meld
        if (!gsm.isHasDoneInitialMeld()) {
            if (newTileCount == 0) {
                // Tidak ada tile baru sama sekali → wajib draw
                showStatusMessage("Harus draw jika tidak meletakkan tile!");
                return;
            }
            // Ada tile baru tapi < 3 → set belum valid
            if (newTileCount < 3) {
                showStatusMessage("Set belum lengkap! Minimal 3 tile dalam satu set.");
                return;
            }
            // Ada >= 3 tile baru → boleh end turn, server yang validasi poin
        }

        // Validasi struktural umum: setiap set minimal 3 tile
        for (int i = 0; i < sets.size(); i++) {
            if (sets.get(i).tile_ids.size() < 3) {
                showStatusMessage("Set #" + (i + 1) + " belum lengkap (min 3 tile)!");
                return;
            }
        }

        // Kirim ke server
        transitionTo(new SubmittingState());
        EndTurnRequest req = gsm.buildEndTurnRequest();
        Gdx.app.log("GameScreen", "Sending end-turn: "
                + sets.size() + " sets, "
                + gsm.getMyRackTiles().size() + " rack tiles");

        facade.endTurn(gameId, req, new ApiCallback<EndTurnResponse>() {
            @Override
            public void onSuccess(EndTurnResponse r) {
                if (r.success) {
                    commandHistory.clear();
                    if (r.data != null && r.data.gameOver) {
                        transitionTo(new GameOverState());
                    } else {
                        transitionTo(new WaitingTurnState());
                        pollGameState();
                    }
                } else {
                    // Server menolak — tampilkan pesan, kembalikan state, biarkan pemain coba lagi
                    String errMsg = (r.error != null) ? r.error : "End turn ditolak";
                    showStatusMessage("Ditolak: " + errMsg);
                    gsm.resetToSnapshot();
                    refreshTileDisplay();
                    transitionTo(new MyTurnState());
                }
            }
            @Override
            public void onFailure(String err) {
                showStatusMessage("Koneksi gagal: " + err);
                gsm.resetToSnapshot();
                refreshTileDisplay();
                transitionTo(new MyTurnState());
            }
        });
    }

    private void showStatusMessage(String msg) {
        statusLabel.setText(msg);
        Gdx.app.log("GameScreen", "Status: " + msg);
    }

    // -------------------------------------------------------------------------
    // Tile display
    // -------------------------------------------------------------------------

    /**
     * Rebuilds all tile actors from the current GameStateManager state.
     * Called after every poll and after undo/reset.
     */
    public void refreshTileDisplay() {
        rebuildRackDisplay();
        rebuildTableDisplay();
        updateTurnInfo();
        updateMeldPointsDisplay();
    }

    public void rebuildRackDisplay() {
        if (rackGroup == null) return;
        // Dispose old actors
        for (Actor a : rackGroup.getChildren()) {
            if (a instanceof TileActor) ((TileActor) a).dispose();
        }
        rackGroup.clearChildren();

        List<TileDto> rack = gsm.getMyRackTiles();
        float tileW  = Constants.TILE_WIDTH + 4f;
        float startX = 10f;
        float tileY  = RACK_Y + (RACK_H - Constants.TILE_HEIGHT) / 2f;

        for (int i = 0; i < rack.size(); i++) {
            TileDto dto = rack.get(i);
            TileActor actor = TileActorFactory.create(dto, new RackTileStrategy());
            actor.setPosition(startX + i * tileW, tileY);
            attachDropListener(actor, "RACK", -1);
            attachDragMoveListener(actor);
            rackGroup.addActor(actor);
        }
    }

    public void rebuildTableDisplay() {
        if (tableGroup == null) return;
        // Dispose old actors
        for (Actor a : tableGroup.getChildren()) {
            if (a instanceof TileActor) ((TileActor) a).dispose();
        }
        tableGroup.clearChildren();
        setBoundingBoxes.clear();
        highlightedSetIndex = -1;

        List<TableSetDto> sets = gsm.getTableSets();
        float setMargin = 12f;
        float tileW     = Constants.TILE_WIDTH * 0.85f + 2f;
        float tileH     = Constants.TILE_HEIGHT * 0.85f;
        float cursorX   = setMargin;
        float tileY     = (TABLE_H - tileH) / 2f;

        for (int si = 0; si < sets.size(); si++) {
            TableSetDto set = sets.get(si);

            if (set.tile_ids == null || set.tile_ids.isEmpty()) {
                Gdx.app.log("GameScreen", "rebuildTableDisplay: set " + si + " is empty, skipping");
                cursorX += setMargin * 2;
                setBoundingBoxes.add(new Rectangle(0, 0, 0, 0)); // placeholder
                continue;
            }

            int tileCount = set.tile_ids.size();
            float setPixelW = tileCount * tileW;

            // Compute bounding box for this set (in tableGroup local coords)
            Rectangle bb = new Rectangle(
                cursorX - BB_PADDING,
                tileY - BB_PADDING,
                setPixelW + BB_PADDING * 2,
                tileH + BB_PADDING * 2
            );
            setBoundingBoxes.add(bb);

            // Determine label text and color based on set contents
            String labelText;
            Color labelColor;
            if (tileCount < 3) {
                labelText = "INCOMPLETE (" + tileCount + ")";
                labelColor = new Color(0.6f, 0.6f, 0.6f, 1f); // Grey
            } else {
                if (set.isNewThisTurn) {
                    set.set_type = gsm.detectSetType(set.tile_ids);
                }
                String detectedType = set.set_type != null ? set.set_type : "RUN";
                if ("GROUP".equals(detectedType)) {
                    // Verify it's a valid group (unique colors)
                    boolean valid = isValidGroupLocally(set.tile_ids);
                    labelText = valid ? "GROUP \u2713" : "GROUP \u2717";
                    labelColor = valid ? new Color(0.2f, 0.85f, 0.2f, 1f) : new Color(0.9f, 0.2f, 0.2f, 1f);
                } else {
                    // Check if it's a valid run (sequential)
                    boolean valid = isValidRunLocally(set.tile_ids);
                    labelText = valid ? "RUN \u2713" : "RUN \u2717";
                    labelColor = valid ? new Color(0.2f, 0.85f, 0.2f, 1f) : new Color(0.9f, 0.2f, 0.2f, 1f);
                }
            }

            Label setLabel = makeLabel(labelText);
            setLabel.setFontScale(0.65f);
            setLabel.setColor(labelColor);
            setLabel.setPosition(cursorX, tileY + tileH + 6);
            tableGroup.addActor(setLabel);

            for (int ti = 0; ti < set.tile_ids.size(); ti++) {
                int tileId = set.tile_ids.get(ti);
                TileDto dto = gsm.getTileById(tileId); // FIX 5: ambil dari cache
                
                if (dto == null) {
                    Gdx.app.log("RENDER_ERR", "TileDto not found in cache for id=" + tileId);
                    continue; 
                }

                TileRenderStrategy strategy = new TableTileStrategy();
                if (!gsm.isHasDoneInitialMeld() && !set.isNewThisTurn) {
                    strategy = new LockedTileStrategy();
                }
                TileActor actor = TileActorFactory.create(dto, strategy);
                actor.setPosition(cursorX + ti * tileW, tileY);
                final int setIndex = si;
                attachDropListener(actor, "TABLE", setIndex);
                attachDragMoveListener(actor);
                tableGroup.addActor(actor);
            }
            cursorX += setPixelW + setMargin * 2;
        }

        // "New set" drop zone at the end
        addNewSetDropZone(cursorX, tileY);
        updateMeldPointsDisplay();
    }

    /**
     * Adds an invisible drop zone that creates a new set when a tile is dropped on it.
     */
    private void addNewSetDropZone(float x, float y) {
        Actor zone = new Actor();
        zone.setBounds(x, y, Constants.TILE_WIDTH, Constants.TILE_HEIGHT);
        zone.addListener(new DragListener() {
            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer) {
                // Drop zone — handled by TileDropEvent on the tile itself
            }
        });
        tableGroup.addActor(zone);
    }

    /**
     * Attaches a TileDropEvent listener to a TileActor that executes the
     * appropriate Command when the tile is dropped.
     *
     * Uses bounding box hit testing to determine which set (if any) the tile
     * lands on. If the tile lands inside a set's bounding box, it joins that set.
     * If it lands on the table but outside all bounding boxes, it creates a new set.
     */
    private void attachDropListener(TileActor actor, String sourceArea, int sourceSetIndex) {
        actor.addListener(new com.badlogic.gdx.scenes.scene2d.EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                if (!(event instanceof TileDropEvent)) return false;
                if (!(currentState instanceof MyTurnState)) return false;

                TileDropEvent drop = (TileDropEvent) event;
                float dropX = drop.dropX;
                float dropY = drop.dropY;

                // Convert stage coords to tableGroup-local coords for bounding box test
                float localDropX = dropX - tableScroll.getX();
                float localDropY = dropY - TABLE_Y;
                // Account for scroll offset
                localDropX += tableScroll.getScrollX();

                int targetSetIndex = findSetIndexAtBoundingBox(localDropX, localDropY);

                Gdx.app.log("DROP", "Tile " + actor.getTileData().id
                    + " src=" + sourceArea + " dropStage=(" + dropX + "," + dropY + ")"
                    + " localDrop=(" + localDropX + "," + localDropY + ")"
                    + " targetBB=" + targetSetIndex);

                if (dropY >= RACK_Y && dropY < RACK_Y + RACK_H) {
                    // Dropped back onto rack
                    if ("TABLE".equals(sourceArea)) {
                        // Blokir drag tile dari set lama ke rack jika belum initial meld
                        if (!gsm.isHasDoneInitialMeld()) {
                            TableSetDto srcSet = gsm.getTableSets().get(sourceSetIndex);
                            if (!srcSet.isNewThisTurn) {
                                showStatusMessage("Set lama tidak boleh disentuh sebelum meld!");
                                return true;
                            }
                        }

                        ReturnTileCommand cmd = new ReturnTileCommand(
                                actor.getTileData().id, sourceSetIndex);
                        commandHistory.execute(cmd);
                        refreshTileDisplay();
                    }
                } else if (dropY >= TABLE_Y && dropY < TABLE_Y + TABLE_H) {
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
                        refreshTileDisplay();
                    } else if ("TABLE".equals(sourceArea)) {
                        if (targetSetIndex >= 0 && targetSetIndex != sourceSetIndex) {
                            MoveWithinTableCommand cmd = new MoveWithinTableCommand(
                                    actor.getTileData().id, sourceSetIndex, targetSetIndex);
                            commandHistory.execute(cmd);
                            refreshTileDisplay();
                        } else if (targetSetIndex == -1) {
                            // Moved to empty space — create new set by returning then placing
                            ReturnTileCommand ret = new ReturnTileCommand(
                                    actor.getTileData().id, sourceSetIndex);
                            commandHistory.execute(ret);
                            PlaceTileCommand place = new PlaceTileCommand(
                                    actor.getTileData().id, true, 0, "RUN");
                            commandHistory.execute(place);
                            refreshTileDisplay();
                        }
                    }
                }
                highlightedSetIndex = -1; // Clear highlight
                return true;
            }
        });
    }

    /**
     * Attaches DragMoveListener to track live tile position during drag
     * and highlight the bounding box of the set being hovered.
     */
    private void attachDragMoveListener(TileActor actor) {
        actor.setDragMoveListener(new TileActor.DragMoveListener() {
            @Override
            public void onDragMove(TileActor a, float stageX, float stageY) {
                if (stageY < TABLE_Y || stageY >= TABLE_Y + TABLE_H) {
                    highlightedSetIndex = -1;
                    return;
                }
                float localX = stageX - tableScroll.getX() + tableScroll.getScrollX();
                float localY = stageY - TABLE_Y;
                highlightedSetIndex = findSetIndexAtBoundingBox(localX, localY);
            }

            @Override
            public void onDragEnd(TileActor a, float stageX, float stageY) {
                highlightedSetIndex = -1;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Bounding box helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the index of the set whose bounding box contains the given
     * tableGroup-local coordinates, or -1 if no match.
     */
    private int findSetIndexAtBoundingBox(float localX, float localY) {
        for (int i = 0; i < setBoundingBoxes.size(); i++) {
            Rectangle bb = setBoundingBoxes.get(i);
            if (bb.width > 0 && bb.contains(localX, localY)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Local validation: checks if tile_ids form a valid GROUP (same number, unique colors).
     */
    private boolean isValidGroupLocally(List<Integer> tileIds) {
        if (tileIds == null || tileIds.size() < 3 || tileIds.size() > 4) return false;
        java.util.Set<String> colors = new java.util.HashSet<>();
        int expectedNumber = -1;
        for (int id : tileIds) {
            TileDto t = findTileById(id);
            if (t == null) return false;
            if (t.isJoker) continue;
            if (expectedNumber == -1) expectedNumber = t.number;
            else if (t.number != expectedNumber) return false;
            if (!colors.add(t.color)) return false; // Duplicate color
        }
        return true;
    }

    /**
     * Local validation: checks if tile_ids form a valid RUN (same color, sequential numbers).
     * Sorts by number before checking sequence.
     */
    private boolean isValidRunLocally(List<Integer> tileIds) {
        if (tileIds == null || tileIds.size() < 3) return false;
        List<TileDto> nonJokers = new ArrayList<>();
        int jokerCount = 0;
        for (int id : tileIds) {
            TileDto t = findTileById(id);
            if (t == null) return false;
            if (t.isJoker) { jokerCount++; continue; }
            nonJokers.add(t);
        }
        if (nonJokers.isEmpty()) return true; // All jokers

        // Check same color
        String color = nonJokers.get(0).color;
        for (TileDto t : nonJokers) {
            if (!color.equals(t.color)) return false;
        }

        // Sort by number and check sequence with joker gaps
        nonJokers.sort((a, b) -> Integer.compare(a.number, b.number));
        int startNum = nonJokers.get(0).number;
        int endNum = nonJokers.get(nonJokers.size() - 1).number;
        int expectedLength = endNum - startNum + 1;
        if (expectedLength != nonJokers.size() + jokerCount) return false;
        if (startNum < 1 || endNum > 13) return false;

        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TileDto findTileById(int id) {
        // Fast O(1) lookup from tile cache (populated by gsm.loadFromServer)
        TileDto cached = gsm.getTileById(id);
        if (cached != null) return cached;

        // Fallback: linear search in rack (covers tiles added locally before next server sync)
        for (TileDto t : gsm.getMyRackTiles()) {
            if (t.id == id) return t;
        }

        // Last resort: create a placeholder DTO so the display doesn't crash
        TileDto placeholder = new TileDto();
        placeholder.id = id;
        placeholder.color = "BLACK";
        placeholder.number = 0;
        placeholder.isJoker = false;
        return placeholder;
    }

    private void updateTurnInfo() {
        String localUser = NetworkManager.getInstance().getCurrentUsername();

        // Update opponent name label (left header) — first participant that isn't us
        for (ParticipantDto p : gsm.getParticipants()) {
            if (!p.username.equals(localUser)) {
                opponentNameLabel.setText(p.username);
                break;
            }
        }

        if (gsm.isMyTurn()) {
            turnInfoLabel.setText("GILIRAN: KAMU");
            turnInfoLabel.setColor(Color.GREEN);
        } else {
            String currentId = gsm.getCurrentTurnUserId();
            String name = resolveUsername(currentId);
            turnInfoLabel.setText("GILIRAN: " + name.toUpperCase());
            turnInfoLabel.setColor(Color.LIGHT_GRAY);
        }
    }

    private void updateMeldPointsDisplay() {
        if (!gsm.isHasDoneInitialMeld()) {
            int points = 0;
            int newTileCount = 0;
            for (TableSetDto set : gsm.getTableSets()) {
                if (set.isNewThisTurn) {
                    newTileCount += set.tile_ids.size();
                    for (int id : set.tile_ids) {
                        TileDto t = gsm.getTileById(id);
                        if (t != null && !t.isJoker) points += t.number;
                    }
                }
            }
            if (newTileCount == 0) {
                statusLabel.setText("Meld: BELUM — harus draw atau taruh tile");
                statusLabel.setColor(Color.YELLOW);
            } else {
                statusLabel.setText("Meld: " + points + "/30 poin");
                statusLabel.setColor(points >= 30 ? Color.GREEN : Color.YELLOW);
            }
        } else {
            statusLabel.setText("Meld: SUDAH ✓");
            statusLabel.setColor(Color.GREEN);
        }
    }

    private String resolveUsername(String userId) {
        if (userId == null) return "?";
        for (ParticipantDto p : gsm.getParticipants()) {
            if (userId.equals(p.userId)) return p.username;
        }
        return userId;
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable makeColorDrawable(Color color) {
        com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(
                1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        com.badlogic.gdx.graphics.Texture tex = new com.badlogic.gdx.graphics.Texture(pm);
        pm.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(tex));
    }

    private ScrollPane.ScrollPaneStyle buildScrollPaneStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.background = makeColorDrawable(new Color(0.10f, 0.20f, 0.10f, 1f));
        return style;
    }

    // -------------------------------------------------------------------------
    // Bounding Box Highlight Rendering
    // -------------------------------------------------------------------------

    @Override
    protected void renderExtra(SpriteBatch batch, ShapeRenderer sr) {
        if (highlightedSetIndex < 0 || highlightedSetIndex >= setBoundingBoxes.size()) return;
        Rectangle bb = setBoundingBoxes.get(highlightedSetIndex);
        if (bb.width <= 0) return;

        // Convert tableGroup-local coords to screen coords
        float screenX = bb.x - tableScroll.getScrollX() + tableScroll.getX();
        float screenY = bb.y + TABLE_Y;

        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                           com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        bbRenderer.begin(ShapeRenderer.ShapeType.Filled);
        bbRenderer.setColor(0.3f, 0.6f, 1f, 0.25f); // Semi-transparent blue
        bbRenderer.rect(screenX, screenY, bb.width, bb.height);
        bbRenderer.end();

        // Draw border
        bbRenderer.begin(ShapeRenderer.ShapeType.Line);
        bbRenderer.setColor(0.4f, 0.7f, 1f, 0.7f); // Brighter blue border
        bbRenderer.rect(screenX, screenY, bb.width, bb.height);
        bbRenderer.end();

        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onDispose() {
        if (tileFont != null) tileFont.dispose();
        if (bbRenderer != null) bbRenderer.dispose();
        // Dispose tile actors — guard against null groups (e.g. if buildUI() never completed)
        if (rackGroup != null) {
            for (Actor a : rackGroup.getChildren()) {
                if (a instanceof TileActor) ((TileActor) a).dispose();
            }
        }
        if (tableGroup != null) {
            for (Actor a : tableGroup.getChildren()) {
                if (a instanceof TileActor) ((TileActor) a).dispose();
            }
        }
    }
}
