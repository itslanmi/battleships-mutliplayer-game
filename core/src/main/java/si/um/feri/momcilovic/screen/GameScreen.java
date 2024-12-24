package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.sql.Time;

import si.um.feri.momcilovic.GameManager;
import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.config.GameConfig;

public class GameScreen extends ScreenAdapter {

    private static final String PREFS_NAME = "settingsPreferences";
    private static final String MOVE_TIME_KEY = "moveTime";
    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;
    private final GameManager gameManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    private int currentPlayer = 1;
    private String[] players;

    private Label playerNameLabel;
    private Label timerLabel;
    private float timer;
    private float turnDuration;
    private String turnDurationString;

    public GameScreen(MomcilovicBattleshipGame game) {
        this.game = game;
        assetManager = game.getAssetManager();
        gameManager = game.getGameManager();
    }

    @Override
    public void show() {
        viewport = new FillViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        players = new String[] {gameManager.getGameResult(gameManager.getGameResults().size()-1).getPlayer(1),
            gameManager.getGameResult(gameManager.getGameResults().size()-1).getPlayer(2)};

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        turnDurationString = prefs.getString(MOVE_TIME_KEY, "MEDIUM (30s)"); // Default to 30 seconds if not set
        switch (turnDurationString) {
            case "HARD (10s)":
                turnDuration = 10;
                break;
            case "MEDIUM (30s)":
                turnDuration = 30;
                break;
            case "EASY (60s)":
                turnDuration = 60;
                break;
        }
        timer = turnDuration;

        createUi();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 0f);

        updateTimer(delta);
        updateUi();
        stage.act(delta);
        stage.draw();
    }

    private void updateTimer(float delta) {
        timer -= delta;
        if (timer <= 0) {
            switchTurn();
        }
    }

    private void switchTurn() {
        currentPlayer = 3 - currentPlayer; // Switch player
        timer = turnDuration;
    }

    private void updateUi() {
        playerNameLabel.setText("Player: " + players[currentPlayer - 1]);
        timerLabel.setText("Time remaining: " + (int) timer + "s");
    }

    private void createUi() {
        // Create and position player name label
        playerNameLabel = new Label("Player: " + players[currentPlayer - 1], skin, "title");
        playerNameLabel.setPosition(20, GameConfig.HUD_HEIGHT - 40);
        stage.addActor(playerNameLabel);

        // Create and position timer label
        timerLabel = new Label("Time remaining: " + (int) timer + "s", skin);
        timerLabel.setPosition(20, GameConfig.HUD_HEIGHT - 80);
        stage.addActor(timerLabel);
    }
}
