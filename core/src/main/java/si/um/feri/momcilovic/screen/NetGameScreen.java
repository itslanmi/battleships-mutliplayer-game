package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;
import si.um.feri.momcilovic.net.NetSession;

/**
 * The LAN battle. Each machine only knows its own fleet; shots and results travel over the connection.
 * A hit gives another shot, a miss passes the turn. TAB shows your own fleet.
 */
public class NetGameScreen extends ScreenAdapter {

    private static final String PREFS_NAME = "settingsPreferences";
    private static final String MOVE_TIME_KEY = "moveTime";
    private static final int TOTAL_HITS = 17;

    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;
    private final NetSession session;
    private final String[] players;
    private final int me;

    private Viewport viewport;
    private Stage stage;
    private Skin skin;
    private TextureAtlas atlas;

    private final int[][] attack = new int[10][10];   // 0 unknown, 1 miss, 2 hit
    private final Image[][] attackCells = new Image[10][10];
    private final Image[][] incomingCells = new Image[10][10];
    private Group attackGroup;
    private Group ownGroup;
    private TextureRegionDrawable hitDrawable;
    private TextureRegionDrawable missDrawable;

    private boolean myTurn;
    private boolean awaitingResult = false;
    private boolean gameOver = false;
    private int myShots, myHits, oppShots, oppHits;
    private float turnDuration = 30;
    private float timer;
    private float leaveTimer = -1;

    private Label nameLabel, turnLabel, timerLabel, shotsLabel, accuracyLabel, scoreLabel, statusLabel, viewLabel;

    public NetGameScreen(MomcilovicBattleshipGame game, NetSession session) {
        this.game = game;
        this.assetManager = game.getAssetManager();
        this.session = session;
        this.players = session.playerNames();
        this.me = session.localIndex();
        this.myTurn = session.isHost; // host shoots first
    }

    @Override
    public void show() {
        viewport = new FillViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());
        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        atlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String t = prefs.getString(MOVE_TIME_KEY, "MEDIUM (30s)");
        turnDuration = t.startsWith("HARD") ? 10 : t.startsWith("EASY") ? 60 : 30;
        timer = turnDuration;

        hitDrawable = new TextureRegionDrawable(atlas.findRegion(RegionNames.GRID_HIT));
        missDrawable = new TextureRegionDrawable(atlas.findRegion(RegionNames.GRID_MISS));
        buildUi();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
        stage.dispose();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 0f);

        if (!gameOver) {
            if (!pollNetwork()) return;
            updateTimer(delta);
        }
        if (leaveTimer >= 0) {
            leaveTimer -= delta;
            if (leaveTimer < 0) {
                session.close();
                game.setScreen(new MenuScreen(game));
                return;
            }
        }

        boolean showOwn = Gdx.input.isKeyPressed(Input.Keys.TAB);
        attackGroup.setVisible(!showOwn);
        ownGroup.setVisible(showOwn);
        viewLabel.setText(showOwn ? "Your fleet" : "Enemy waters (hold TAB: your fleet)");
        updateLabels();

        stage.act(delta);
        stage.draw();
    }

    // -------------------------------------------------------------- game logic

    private boolean pollNetwork() {
        String line;
        while ((line = session.connection.poll()) != null) {
            String[] p = line.split(" ");
            switch (p[0]) {
                case "SHOT":
                    if (p.length == 3 && myTurn == false) {
                        handleIncomingShot(parse(p[1]), parse(p[2]));
                        if (gameOver) return false;
                    }
                    break;
                case "RESULT":
                    if (p.length >= 4 && awaitingResult) {
                        handleResult(parse(p[1]), parse(p[2]), "HIT".equals(p[3]), p.length > 4 && "SUNK".equals(p[4]));
                        if (gameOver) return false;
                    }
                    break;
                case "PASS":
                    startMyTurn();
                    break;
                case "LEAVE":
                    statusLabel.setText("Opponent left the game.");
                    gameOver = true;
                    leaveTimer = 3f;
                    return true;
                default:
                    break;
            }
        }
        return true;
    }

    private static int parse(String s) {
        try {
            int v = Integer.parseInt(s);
            return v < 0 || v > 9 ? -1 : v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void handleIncomingShot(int r, int c) {
        if (r < 0 || c < 0 || incomingCells[r][c].getColor().a > 0) return; // invalid or repeated shot
        int result = session.receiveShot(r, c);
        oppShots++;
        Image cell = incomingCells[r][c];
        cell.setDrawable(result == 0 ? missDrawable : hitDrawable);
        cell.getColor().a = 1;
        session.connection.send("RESULT " + r + " " + c + (result == 0 ? " MISS" : " HIT") + (result == 2 ? " SUNK" : ""));
        if (result != 0) {
            oppHits++;
            statusLabel.setText(result == 2 ? "Your ship was sunk!" : "Your ship was hit!");
            if (oppHits == TOTAL_HITS) {
                finish(false);
            }
        } else {
            statusLabel.setText("Opponent missed.");
            startMyTurn();
        }
    }

    private void handleResult(int r, int c, boolean hit, boolean sunk) {
        awaitingResult = false;
        if (r < 0 || c < 0) return;
        myShots++;
        attack[r][c] = hit ? 2 : 1;
        Image cell = attackCells[r][c];
        cell.setDrawable(hit ? hitDrawable : missDrawable);
        cell.getColor().a = 1;
        if (hit) {
            myHits++;
            statusLabel.setText(sunk ? "You sunk a ship!" : "Hit! Shoot again.");
            timer = turnDuration;
            if (myHits == TOTAL_HITS) {
                finish(true);
            }
        } else {
            statusLabel.setText("Miss.");
            myTurn = false;
            timer = turnDuration;
        }
    }

    private void startMyTurn() {
        myTurn = true;
        timer = turnDuration;
    }

    private void updateTimer(float delta) {
        timer -= delta;
        if (timer <= 0) {
            if (myTurn && !awaitingResult) {
                session.connection.send("PASS");
                myTurn = false;
                statusLabel.setText("Out of time - turn passed.");
            }
            timer = turnDuration;
        }
    }

    private void fire(int r, int c) {
        if (!myTurn || awaitingResult || gameOver || attack[r][c] != 0) return;
        awaitingResult = true;
        session.connection.send("SHOT " + r + " " + c);
    }

    private void finish(boolean iWon) {
        gameOver = true;
        float[] accuracies = new float[2];
        float[] scores = new float[2];
        int opp = 1 - me;
        accuracies[me] = myShots == 0 ? 0 : myHits * 100f / myShots;
        accuracies[opp] = oppShots == 0 ? 0 : oppHits * 100f / oppShots;
        scores[me] = myHits * 100 + (iWon ? 1000 : 0);
        scores[opp] = oppHits * 100 + (iWon ? 0 : 1000);
        session.close();
        game.setScreen(new GameOverScreen(game, players, accuracies, scores, true, iWon ? me : opp));
    }

    // ---------------------------------------------------------------------- UI

    private void buildUi() {
        Image background = new Image(atlas.findRegion(RegionNames.BACKGROUND));
        background.setFillParent(true);
        stage.addActor(background);

        Image grid = new Image(atlas.findRegion(RegionNames.GRID));
        grid.setPosition(4 * GameConfig.CELL_SIZE, 2 * GameConfig.CELL_SIZE);
        stage.addActor(grid);

        attackGroup = new Group();
        ownGroup = new Group();
        ownGroup.setVisible(false);

        for (NetSession.Placement pl : session.placements) {
            Image boat = new Image(new TextureRegionDrawable(atlas.findRegion(pl.region)));
            float x = (5 + pl.col) * GameConfig.CELL_SIZE;
            if (pl.horizontal) {
                boat.setPosition(x, (11 - pl.row) * GameConfig.CELL_SIZE);
            } else {
                boat.setPosition(x, (12 - pl.row) * GameConfig.CELL_SIZE);
                boat.setRotation(-90);
            }
            ownGroup.addActor(boat);
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                float x = (5 + j) * GameConfig.CELL_SIZE;
                float y = (11 - i) * GameConfig.CELL_SIZE;

                Image cell = new Image(missDrawable);
                cell.setPosition(x, y);
                cell.getColor().a = 0; // still receives clicks while invisible
                final int r = i, c = j;
                cell.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float px, float py) {
                        fire(r, c);
                    }
                });
                attackCells[i][j] = cell;
                attackGroup.addActor(cell);

                Image in = new Image(missDrawable);
                in.setPosition(x, y);
                in.getColor().a = 0;
                incomingCells[i][j] = in;
                ownGroup.addActor(in);
            }
        }
        stage.addActor(attackGroup);
        stage.addActor(ownGroup);

        float cs = GameConfig.CELL_SIZE;
        nameLabel = new Label(players[me], skin, "title");
        nameLabel.setPosition(10 * cs, GameConfig.HUD_HEIGHT - cs - 10);
        stage.addActor(nameLabel);

        turnLabel = new Label("", skin);
        turnLabel.setPosition(cs, 12 * cs);
        stage.addActor(turnLabel);
        timerLabel = new Label("", skin);
        timerLabel.setPosition(cs, 10 * cs);
        stage.addActor(timerLabel);
        shotsLabel = new Label("", skin);
        shotsLabel.setPosition(cs, 8 * cs);
        stage.addActor(shotsLabel);
        accuracyLabel = new Label("", skin);
        accuracyLabel.setPosition(cs, 6 * cs);
        stage.addActor(accuracyLabel);
        scoreLabel = new Label("", skin);
        scoreLabel.setPosition(cs, 4 * cs);
        stage.addActor(scoreLabel);

        viewLabel = new Label("", skin);
        viewLabel.setPosition(5 * cs, 1.2f * cs);
        stage.addActor(viewLabel);
        statusLabel = new Label(myTurn ? "You shoot first!" : "Opponent shoots first.", skin);
        statusLabel.setPosition(5 * cs, 0.4f * cs);
        stage.addActor(statusLabel);
    }

    private void updateLabels() {
        turnLabel.setText(gameOver ? "" : myTurn ? "YOUR TURN" : players[1 - me] + "'s turn");
        timerLabel.setText("Time: \n" + Math.max(0, (int) timer) + "s");
        timerLabel.setColor(timer < 5 ? 1 : 1, timer < 5 ? 0 : 1, timer < 5 ? 0 : 1, 1);
        shotsLabel.setText("Shots: \n" + myShots);
        accuracyLabel.setText("Accuracy: \n" + (myShots == 0 ? 0 : Math.round(myHits * 100f / myShots)) + "%");
        scoreLabel.setText("Hits: \n" + myHits + " / " + TOTAL_HITS);
    }
}
