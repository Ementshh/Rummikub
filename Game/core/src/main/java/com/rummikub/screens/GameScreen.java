package com.rummikub.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.rummikub.RummikubGame;
import com.rummikub.actors.TileActor;
import com.rummikub.command.CommandHistory;
import com.rummikub.factory.TileActorFactory;
import com.rummikub.network.ApiCallback;
import com.rummikub.network.GameApiFacade;
import com.rummikub.network.NetworkManager;
import com.rummikub.network.dto.*;
import com.rummikub.screens.components.*;
import com.rummikub.screens.states.*;
import com.rummikub.state.GameStateManager;
import com.rummikub.strategy.RackTileStrategy;
import com.rummikub.utils.Constants;

import java.util.List;

/**
 * Main game screen — acts as an orchestrator that delegates to focused components:
 * <ul>
 *   <li>{@link TableRenderer} — table tile rendering and bounding boxes</li>
 *   <li>{@link TileDragHandler} — drag-and-drop logic and command execution</li>
 *   <li>{@link SetValidator} — client-side set validation</li>
 *   <li>{@link GameHudManager} — HUD label updates</li>
 * </ul>
 *
 * Integrates the State Machine (WaitingTurnState / MyTurnState /
 * SubmittingState / GameOverState) and the Command pattern for undo.
 */
public class GameScreen extends BaseScreen implements TileDragHandler.Callback {

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
    // Components (delegated responsibilities)
    // -------------------------------------------------------------------------
    private TableRenderer tableRenderer;
    private TileDragHandler dragHandler;
    private SetValidator setValidator;
    private GameHudManager hudManager;

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------
    private GameScreenState currentState;

    // -------------------------------------------------------------------------
    // UI components (owned by this screen, passed to components)
    // -------------------------------------------------------------------------
    private TextButton drawButton;
    private TextButton resetButton;
    private TextButton endTurnButton;
    private TextButton sortByNumberButton;
    private TextButton sortByColorButton;
    private Label waitingOverlay;
    private Actor rackForbiddenOverlay;

    private Group rackGroup;
    private Group tableGroup;
    private ScrollPane tableScroll;

    private BitmapFont tileFont;
    private ShapeRenderer bbRenderer;

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

        // Initialize components after UI elements are created
        setValidator = new SetValidator(gsm);
        tableRenderer = new TableRenderer(tableGroup, tableScroll, gsm, setValidator, TABLE_H);
        dragHandler = new TileDragHandler(RACK_Y, RACK_H, TABLE_Y, TABLE_H,
                                          gsm, commandHistory, tableRenderer, this);

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

        // Show the local player's own username in the top-left
        String localUser = NetworkManager.getInstance().getCurrentUsername();
        Label playerNameLabel = makeLabel(localUser != null ? localUser : "Pemain");
        Label turnInfoLabel  = makeLabel("Giliran: ...");
        Label timerLabel = makeLabel("TIMER: --:--");
        timerLabel.setColor(Color.YELLOW);

        header.add(playerNameLabel).expandX().left();
        header.add(turnInfoLabel).expandX().center();
        header.add(timerLabel).right().padRight(20);

        stage.addActor(header);

        // Store references for HUD manager (created later in buildUI)
        // We use a deferred approach: labels are stored in fields temporarily
        this._timerLabel = timerLabel;
        this._turnInfoLabel = turnInfoLabel;
        this._playerNameLabel = playerNameLabel;
    }

    // Temporary label references for deferred HudManager construction
    private Label _timerLabel, _turnInfoLabel, _playerNameLabel, _statusLabel;

    private void buildTableArea() {
        // Scrollable horizontal area for table sets
        tableGroup = new Group();
        tableGroup.setSize(3000, TABLE_H - 20); // wide enough for many sets

        tableScroll = new ScrollPane(tableGroup, buildScrollPaneStyle());
        tableScroll.setBounds(0, TABLE_Y, Constants.SCREEN_WIDTH, TABLE_H);
        tableScroll.setScrollingDisabled(false, true);
        tableScroll.setFadeScrollBars(false);
        tableScroll.setCancelTouchFocus(false);
        tableScroll.setFlickScroll(false); // Matikan scroll bawaan (kiri)

        // Implementasi scroll manual menggunakan klik kanan
        tableScroll.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            private float lastX;
            private boolean isDraggingRight = false;

            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                if (button == com.badlogic.gdx.Input.Buttons.RIGHT) {
                    lastX = x;
                    isDraggingRight = true;
                    return true;
                }
                return false;
            }

            @Override
            public void touchDragged(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer) {
                if (isDraggingRight) {
                    float deltaX = x - lastX;
                    tableScroll.setScrollX(tableScroll.getScrollX() - deltaX);
                    lastX = x;
                }
            }

            @Override
            public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                if (button == com.badlogic.gdx.Input.Buttons.RIGHT) {
                    isDraggingRight = false;
                }
            }
        });

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
        _statusLabel = makeLabel("Meld: BELUM (min 30 poin)");
        _statusLabel.setColor(Color.ORANGE);

        // All buttons disabled by default — enabled only when MyTurnState is active
        drawButton.setDisabled(true);
        resetButton.setDisabled(true);
        endTurnButton.setDisabled(true);

        // Sort buttons
        sortByNumberButton = makeButton("SORT NUM", new Color(0.25f, 0.35f, 0.50f, 1f));
        sortByColorButton = makeButton("SORT CLR", new Color(0.45f, 0.30f, 0.50f, 1f));
        sortByNumberButton.setDisabled(true);
        sortByColorButton.setDisabled(true);

        bar.add(drawButton).width(120).height(44).padRight(12);
        bar.add(resetButton).width(120).height(44).padRight(12);
        bar.add(endTurnButton).width(140).height(44).padRight(12);
        bar.add(sortByNumberButton).width(100).height(44).padRight(8);
        bar.add(sortByColorButton).width(100).height(44).padRight(20);
        bar.add(_statusLabel).expandX().left();

        stage.addActor(bar);

        // Create HudManager now that all labels exist
        hudManager = new GameHudManager(_timerLabel, _turnInfoLabel,
                                        _statusLabel, _playerNameLabel, gsm);

        // Listeners
        drawButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { onDrawClicked(); }
        });
        resetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { onResetClicked(); }
        });
        endTurnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { onEndTurnClicked(); }
        });
        sortByNumberButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { onSortByNumberClicked(); }
        });
        sortByColorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { onSortByColorClicked(); }
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

        rackForbiddenOverlay = buildRackForbiddenOverlay();
        stage.addActor(rackForbiddenOverlay);
    }

    private Actor buildRackForbiddenOverlay() {
        // Use a Table so we can center a label inside it
        Table overlay = new Table();
        overlay.setBounds(0, RACK_Y, Constants.SCREEN_WIDTH, RACK_H);
        overlay.setBackground(makeColorDrawable(new Color(0.7f, 0.05f, 0.05f, 0.80f)));

        Label forbidLabel = makeLabel("⛔  Tidak bisa diletakkan di sini");
        forbidLabel.setFontScale(1.1f);
        forbidLabel.setColor(Color.WHITE);
        overlay.add(forbidLabel).center();

        overlay.setVisible(false);
        // Non-interactive — never consumes touch events
        overlay.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        return overlay;
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
    // TileDragHandler.Callback implementation
    // -------------------------------------------------------------------------

    @Override
    public GameScreenState getCurrentState() { return currentState; }

    @Override
    public void showStatusMessage(String msg) { hudManager.showStatusMessage(msg); }

    @Override
    public void onTableTileDragStart() {
        if (rackForbiddenOverlay != null) rackForbiddenOverlay.setVisible(true);
    }

    @Override
    public void onTableTileDragEnd() {
        if (rackForbiddenOverlay != null) rackForbiddenOverlay.setVisible(false);
    }

    // -------------------------------------------------------------------------
    // State callbacks (called by state objects)
    // -------------------------------------------------------------------------

    
    // Atur control berdasarkan statenya bisa apa
    // Setiap state atur dia boleh ngapain aja
    public void applyStatePermissions() {
        if (currentState == null) return;

        // Game-action buttons: enable klo statenya boleh game actions
        boolean gameActions = currentState.canUseGameActions();
        drawButton.setDisabled(!gameActions);
        resetButton.setDisabled(!gameActions);
        endTurnButton.setDisabled(!gameActions);

        // Sort buttons: enable kalo state boleh sorting
        boolean canSort = currentState.canSortRack();
        sortByNumberButton.setDisabled(!canSort);
        sortByColorButton.setDisabled(!canSort);

        // Waiting overlay
        waitingOverlay.setVisible(!gameActions);

        // Rack tiles
        com.badlogic.gdx.scenes.scene2d.Touchable rackTouchable = currentState.canInteractWithRack()
                ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                : com.badlogic.gdx.scenes.scene2d.Touchable.disabled;
        for (Actor a : rackGroup.getChildren()) {
            a.setTouchable(rackTouchable);
        }

        // Table tiles: touchable klo statenya boleh table interaction
        com.badlogic.gdx.scenes.scene2d.Touchable tableTouchable = currentState.canInteractWithTable()
                ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                : com.badlogic.gdx.scenes.scene2d.Touchable.disabled;
        for (Actor a : tableGroup.getChildren()) {
            a.setTouchable(tableTouchable);
        }
    }

    /** Polls the server for the latest game state. */
    public void pollGameState() {
        facade.getGameState(gameId, new ApiCallback<GameStateResponse>() {
            @Override
            public void onSuccess(GameStateResponse r) {
                if (r == null || !r.success || r.data == null) return;

                if ("FINISHED".equals(r.data.status)) {
                    gsm.loadFromServer(r.data, setValidator);
                    transitionTo(new GameOverState());
                    return;
                }

                gsm.loadFromServer(r.data, setValidator);

                // BUG 2 DEBUG
                Gdx.app.log("DEBUG_TURN", "currentTurnUserId dari server: " + r.data.currentTurnUserId);
                Gdx.app.log("DEBUG_TURN", "userId kita (NetworkManager): " + NetworkManager.getInstance().getUserId());
                Gdx.app.log("DEBUG_TURN", "isMyTurn() result: " + gsm.isMyTurn());

                refreshTileDisplay();
                hudManager.updateMeldPointsDisplay();

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
        hudManager.updateTimerDisplay(seconds);
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
                hudManager.showStatusMessage("Harus taruh set baru (≥30 poin) atau draw — belum bisa manipulasi set lama!");
                return;
            }
            if (newTileCount < 3) {
                hudManager.showStatusMessage("Set belum lengkap! Minimal 3 tile dalam satu set.");
                return;
            }
        }

        // setiap set minimal 3 tile (skip empty gaps)
        for (int i = 0; i < sets.size(); i++) {
            TableSetDto set = sets.get(i);
            if (set.isEmpty()) continue;  // Skip gap
            if (set.tile_ids.size() < 3) {
                hudManager.showStatusMessage("Set #" + (i + 1) + " belum lengkap (min 3 tile)!");
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
                    String errMsg = (r.error != null) ? r.error : "End turn ditolak";
                    hudManager.showStatusMessage("Ditolak: " + errMsg);
                    gsm.resetToSnapshot();
                    refreshTileDisplay();
                    transitionTo(new MyTurnState());
                }
            }
            @Override
            public void onFailure(String err) {
                hudManager.showStatusMessage("Koneksi gagal: " + err);
                gsm.resetToSnapshot();
                refreshTileDisplay();
                transitionTo(new MyTurnState());
            }
        });
    }

    private void onSortByNumberClicked() {
        gsm.sortRackTilesByNumber();
        refreshTileDisplay();
    }

    private void onSortByColorClicked() {
        gsm.sortRackTilesByColor();
        refreshTileDisplay();
    }

    // -------------------------------------------------------------------------
    // Tile display
    // -------------------------------------------------------------------------

    /**
     * Rebuilds all tile actors from the current GameStateManager state.
     * Orchestrates calls to the component renderers and HUD manager.
     * Always re-applies the correct Touchable state to newly created actors.
     * Also syncs TableRenderer columns with TableGridManager.
     */
    public void refreshTileDisplay() {
        // Check grid expansion after tile placement
        gsm.notifyTilePlacement(setValidator);
        // Sync TableRenderer columns with GridManager
        tableRenderer.setNumColumns(gsm.getGridColumns());

        rebuildRackDisplay();
        tableRenderer.rebuild(dragHandler);
        hudManager.updateTurnInfo();
        hudManager.updateMeldPointsDisplay();
        // Re-apply the current state's touchable setting to ALL newly created actors
        applyTouchableToTiles();
    }

    private void applyTouchableToTiles() {
        if (currentState == null) return;

        com.badlogic.gdx.scenes.scene2d.Touchable rackTouchable = currentState.canInteractWithRack()
                ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                : com.badlogic.gdx.scenes.scene2d.Touchable.disabled;
        com.badlogic.gdx.scenes.scene2d.Touchable tableTouchable = currentState.canInteractWithTable()
                ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                : com.badlogic.gdx.scenes.scene2d.Touchable.disabled;

        if (rackGroup != null) {
            for (Actor a : rackGroup.getChildren()) {
                a.setTouchable(rackTouchable);
            }
        }
        if (tableGroup != null) {
            for (Actor a : tableGroup.getChildren()) {
                a.setTouchable(tableTouchable);
            }
        }
    }

    private void rebuildRackDisplay() {
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
            dragHandler.attachDropListener(actor, "RACK", -1, false);
            dragHandler.attachDragMoveListener(actor, "RACK", false);
            rackGroup.addActor(actor);
        }
    }

    // -------------------------------------------------------------------------
    // Bounding Box Highlight Rendering (delegated to TableRenderer)
    // -------------------------------------------------------------------------

    @Override
    protected void renderExtra(SpriteBatch batch, ShapeRenderer sr) {
        tableRenderer.renderHighlight(bbRenderer, TABLE_Y);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onDispose() {
        if (tileFont != null) tileFont.dispose();
        if (bbRenderer != null) bbRenderer.dispose();
        if (tableRenderer != null) tableRenderer.dispose();
        if (rackGroup != null) {
            for (Actor a : rackGroup.getChildren()) {
                if (a instanceof TileActor) ((TileActor) a).dispose();
            }
        }
    }
}
