package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Objects;

import si.um.feri.momcilovic.GameManager;
import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;

public class GameScreen extends ScreenAdapter {

    private static final String PREFS_NAME = "settingsPreferences";
    private static final String MOVE_TIME_KEY = "moveTime";
    private static Boolean isClickEnabled = true;
    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;
    private final GameManager gameManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    private int currentPlayer = 1;
    private String[] players;
    private int[] playersHealth = {17, 17};
    private int[] playersShots = {0, 0};
    private float[] playersScores = {0, 0};
    private float[] accuracies = {0, 0};
    private long turnStartTime;

    private int[][] player1Matrix = new int[10][10];
    private int[][] player2Matrix = new int[10][10];

    private boolean[][] player1GridState = new boolean[10][10];
    private boolean[][] player2GridState = new boolean[10][10];

    private Label playerNameLabel;
    private Label timerLabel;
    private Label shotsLabel;
    private Label accuracyLabel;
    private Label scoreLabel;
    private float timer;
    private float turnDuration;

    public GameScreen(MomcilovicBattleshipGame game, String[] playersNames, int player1[][], int player2[][]) {
        this.game = game;
        assetManager = game.getAssetManager();
        gameManager = game.getGameManager();
        players = playersNames;
        player1Matrix = player1;
        player2Matrix = player2;
    }

    @Override
    public void show() {
        viewport = new FillViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String turnDurationString = prefs.getString(MOVE_TIME_KEY, "MEDIUM (30s)"); // Default to 30 seconds if not set
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
        turnStartTime = System.currentTimeMillis();

        createUi();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 0f);

        if (playersHealth[0] == 0 || playersHealth[1] == 0) {
            if(playersHealth[0] == 0){
                playersScores[1] += 1000;
            }else {
                playersScores[0] += 1000;
            }
            game.setScreen(new GameOverScreen(game, players, accuracies, playersScores));
        }

        updateTimer(delta);
        updateUi();
        stage.act(delta);
        stage.draw();
    }

    private void updateTimer(float delta) {
        timer -= delta;
        if (timer < 5) {
            timerLabel.setColor(1, 0, 0, 1);
        }
        if (timer <= 0) {
            switchTurn();
        }
    }

    private void switchTurn() {
        currentPlayer = 3 - currentPlayer; // Switch player
        timer = turnDuration;
        turnStartTime = System.currentTimeMillis();
        isClickEnabled = true;
        updateGridImages();
    }

    private void updateGridImages() {
        stage.clear();
        createUi();
    }

    private void updateUi() {
        playerNameLabel.setText(players[currentPlayer - 1]);
        timerLabel.setText("Time: " + (int) timer + "s");
        shotsLabel.setText("Shots: \n" + playersShots[currentPlayer - 1]);
        if (playersShots[currentPlayer - 1] == 0) {
            accuracyLabel.setText("Accuracy: \n" + 0 + "%");
        } else {
            accuracies[currentPlayer - 1] = ((17 - playersHealth[3 - currentPlayer - 1]) * 100.0f) / playersShots[currentPlayer - 1];
            accuracyLabel.setText("Accuracy: \n" + Math.round(accuracies[currentPlayer - 1]) + "%");
        }
        scoreLabel.setText("Score: \n" + playersScores[currentPlayer - 1]);
    }

    private void createUi() {
        // Add background image
        Image backgroundImage = new Image(gameplayAtlas.findRegion(RegionNames.BACKGROUND));
        backgroundImage.setFillParent(true);
        stage.addActor(backgroundImage);

        // Add a specific image at certain coordinates
        Image specificImage = new Image(gameplayAtlas.findRegion(RegionNames.GRID));
        specificImage.setPosition(4 * GameConfig.CELL_SIZE, 2 * GameConfig.CELL_SIZE); // Set the desired coordinates
        stage.addActor(specificImage);

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Image gridImage;
                if (currentPlayer == 1) {
                    if (player2Matrix[i][j] == 1) {
                        gridImage = new Image(gameplayAtlas.findRegion(RegionNames.GRID_HIT));
                        gridImage.setName("GRID_HIT");
                    } else {
                        gridImage = new Image(gameplayAtlas.findRegion(RegionNames.GRID_MISS));
                    }
                } else {
                    if (player1Matrix[i][j] == 1) {
                        gridImage = new Image(gameplayAtlas.findRegion(RegionNames.GRID_HIT));
                        gridImage.setName("GRID_HIT");
                    } else {
                        gridImage = new Image(gameplayAtlas.findRegion(RegionNames.GRID_MISS));
                    }
                }
                gridImage.setPosition((5 + j) * GameConfig.CELL_SIZE, (11 - i) * GameConfig.CELL_SIZE);
                if ((currentPlayer == 1 && player2GridState[i][j]) || (currentPlayer == 2 && player1GridState[i][j])) {
                    gridImage.getColor().a = 1; // Set alpha to 1 (100%) if previously opened
                } else {
                    gridImage.getColor().a = 0; // Set initial alpha to 0
                }
                int finalI = i;
                int finalJ = j;
                gridImage.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if ((currentPlayer == 1 && player2GridState[finalI][finalJ]) || (currentPlayer == 2 && player1GridState[finalI][finalJ]) || !isClickEnabled) {
                            return;
                        }
                        gridImage.getColor().a = 1; // Set alpha to 1 (100%) on click
                        if (currentPlayer == 1) {
                            player2GridState[finalI][finalJ] = true;
                        } else {
                            player1GridState[finalI][finalJ] = true;
                        }
                        playersShots[currentPlayer - 1]++;
                        if (Objects.equals(gridImage.getName(), "GRID_HIT")) {
                            playersHealth[3 - currentPlayer - 1]--;
                            updateScore();
                        }
                        //isClickEnabled = false;
                        timer = 2;
                        System.out.println("Player " + players[3 - currentPlayer - 1] + "," + playersHealth[3 - currentPlayer - 1] + " health left");
                    }
                });
                stage.addActor(gridImage);
            }
        }

        // Create and position player name label
        playerNameLabel = new Label(players[currentPlayer - 1], skin, "title");
        playerNameLabel.setPosition(10 * GameConfig.CELL_SIZE, GameConfig.HUD_HEIGHT - GameConfig.CELL_SIZE - 10);
        stage.addActor(playerNameLabel);

        // Create and position timer label
        timerLabel = new Label("Time: \n" + (int) timer + "s", skin);
        timerLabel.setPosition(GameConfig.CELL_SIZE, 11 * GameConfig.CELL_SIZE);
        stage.addActor(timerLabel);

        // Create and position shots label
        shotsLabel = new Label("Shots: \n" + playersShots[currentPlayer - 1], skin);
        shotsLabel.setPosition(GameConfig.CELL_SIZE, 9 * GameConfig.CELL_SIZE);
        stage.addActor(shotsLabel);

        // Create and position accuracy label
        if (playersShots[currentPlayer - 1] == 0) {
            accuracyLabel = new Label("Accuracy: \n" + 0 + "%", skin);
        } else {
            accuracyLabel = new Label("Accuracy: \n" + ((17 - playersHealth[3 - currentPlayer - 1]) * 100 / playersShots[currentPlayer - 1]) + "%", skin);
        }
        accuracyLabel.setPosition(GameConfig.CELL_SIZE, 7 * GameConfig.CELL_SIZE);
        stage.addActor(accuracyLabel);

        // Create and position score label
        scoreLabel = new Label("Score: \n" + playersScores[currentPlayer - 1], skin);
        scoreLabel.setPosition(GameConfig.CELL_SIZE, 5 * GameConfig.CELL_SIZE);
        stage.addActor(scoreLabel);
    }

    private void updateScore() {
        long currentTime = System.currentTimeMillis();
        long timeTaken = currentTime - turnStartTime;
        float accuracy = playersShots[currentPlayer - 1] == 0 ? 0 : (float) ((17 - playersHealth[3 - currentPlayer - 1]) * 100) / playersShots[currentPlayer - 1];
        float score = (turnDuration * 1000 - timeTaken) * (accuracy / 1000.0f);
        playersScores[currentPlayer - 1] += Math.round(score);
    }
}
