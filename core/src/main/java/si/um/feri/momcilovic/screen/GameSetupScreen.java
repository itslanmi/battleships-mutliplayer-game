package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Arrays;

import si.um.feri.momcilovic.GameManager;
import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;
import si.um.feri.momcilovic.net.NetSession;

public class GameSetupScreen extends ScreenAdapter {

    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;
    private final GameManager gameManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    private int selectedRow = 0;
    private int selectedCol = 0;
    private boolean isHorizontal = true;
    private int currentPlayer = 1;
    private final String[] players;
    private final NetSession session; // null = hot-seat game on one PC
    private boolean waitingForOpponent = false;
    private boolean opponentReady = false;

    private Image[] boatImages;
    private final String[] boats = {RegionNames.GRID_BATTLESHIP, RegionNames.GRID_CRUISER, RegionNames.GRID_DESTROYER, RegionNames.GRID_SUBMARINE, RegionNames.GRID_PATROL};

    private final int[][] player1Matrix = new int[10][10];
    private final int[][] player2Matrix = new int[10][10];
    private int currentBoatIndex = 0;
    private int entersPressed = 0;

    private int minRow = 2;
    private int maxRow = 11;

    private int minCol = 5;
    private int maxCol = 14;

    private Label playerNameLabel;
    private Label boatInfoLabel;
    private Label orientationLabel;


    public GameSetupScreen(MomcilovicBattleshipGame game, String[] playersNames) {
        this.game = game;
        assetManager = game.getAssetManager();
        gameManager = game.getGameManager();
        players = playersNames;
        session = null;
    }

    /** LAN game: only the local player places ships. */
    public GameSetupScreen(MomcilovicBattleshipGame game, NetSession session) {
        this.game = game;
        assetManager = game.getAssetManager();
        gameManager = game.getGameManager();
        players = new String[]{session.localName, session.remoteName};
        this.session = session;
    }

    @Override
    public void show() {
        viewport = new FillViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        boatImages = new Image[boats.length];
        for (int i = 0; i < boats.length; i++) {
            boatImages[i] = new Image(new TextureRegionDrawable(gameplayAtlas.findRegion(boats[i])));
            boatImages[i].setPosition(0, -64);
        }
        boatImages[0].setPosition(5 * 64, 11 * 64);

        for (int[] matrix : player1Matrix) {
            Arrays.fill(matrix, 0);
        }
        for (int[] matrix : player2Matrix) {
            Arrays.fill(matrix, 0);
        }

        createUi();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 0f);

        if (session != null && !pollNetwork()) {
            return;
        }
        if (!waitingForOpponent) {
            handleInput();
        }
        updateUi();
        stage.act(delta);
        stage.draw();
    }

    private void changeLimits() {
        if (isHorizontal) {
            minRow = 2;
            maxRow = 11;
            minCol = 5;
            maxCol = 15 - (int) (boatImages[currentBoatIndex].getWidth() / 64);
        } else {
            minRow = 2 + (int) (boatImages[currentBoatIndex].getWidth() / 64);
            maxRow = 12;
            minCol = 5;
            maxCol = 14;
        }
    }

    private void setupBoats() {
        for (Image boatImage : boatImages) {
            boatImage.setPosition(0, -64);
            boatImage.setRotation(0);
        }
        boatImages[0].setPosition(5 * 64, 11 * 64);
    }

    private void handleInput() {
        changeLimits();
        float cellSize = GameConfig.CELL_SIZE;

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            if (boatImages[currentBoatIndex].getY() < maxRow * cellSize) {
                boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), boatImages[currentBoatIndex].getY() + cellSize);
                selectedRow--;
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            if (boatImages[currentBoatIndex].getY() > minRow * cellSize) {
                boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), boatImages[currentBoatIndex].getY() - cellSize);
                selectedRow++;
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            if (boatImages[currentBoatIndex].getX() > minCol * cellSize) {
                boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX() - cellSize, boatImages[currentBoatIndex].getY());
                selectedCol--;
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            if (boatImages[currentBoatIndex].getX() < maxCol * cellSize) {
                boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX() + cellSize, boatImages[currentBoatIndex].getY());
                selectedCol++;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if (boatImages[currentBoatIndex].getRotation() == 0) {
                boatImages[currentBoatIndex].rotateBy(-90);
                if (boatImages[currentBoatIndex].getY() - boatImages[currentBoatIndex].getImageWidth() < minRow * cellSize) {
                    boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), minRow * cellSize + boatImages[currentBoatIndex].getImageWidth());
                } else {
                    boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), boatImages[currentBoatIndex].getY() + cellSize);
                }
            } else {
                boatImages[currentBoatIndex].rotateBy(90);
                if (boatImages[currentBoatIndex].getX() + boatImages[currentBoatIndex].getImageWidth() > maxCol * cellSize) {
                    boatImages[currentBoatIndex].setPosition(maxCol * cellSize - boatImages[currentBoatIndex].getImageWidth() + cellSize, boatImages[currentBoatIndex].getY() - cellSize);
                } else {
                    boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), boatImages[currentBoatIndex].getY() - cellSize);
                }
            }
            isHorizontal = !isHorizontal;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            try {
                Image boat = boatImages[currentBoatIndex];
                int boatSize = (int) (boat.getImageWidth() / GameConfig.CELL_SIZE);
                // derive the cell from where the boat actually is on screen
                int col = Math.round(boat.getX() / cellSize) - 5;
                int row = (isHorizontal ? 11 : 12) - Math.round(boat.getY() / cellSize);
                placeBoatInMatrix(currentPlayer == 1 ? player1Matrix : player2Matrix, row, col, isHorizontal, boatSize);
                selectedRow = row;
                selectedCol = col;
                entersPressed++;
                if (session != null) {
                    session.placements.add(new NetSession.Placement(boats[currentBoatIndex], row, col, isHorizontal, boatSize));
                    if (entersPressed == boats.length) {
                        finishNetPlacement();
                        return;
                    }
                }
                placeBoat();
                isHorizontal = true;
                selectedCol = 0;
                selectedRow = 0;
                changeLimits();
                boatImages[currentBoatIndex].setPosition(minCol * cellSize, maxRow * cellSize);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            if (session == null && entersPressed == 10) {
                game.setScreen(new GameScreen(game, players, player1Matrix, player2Matrix));
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            for (int[] matrix : player1Matrix) {
                System.out.println(Arrays.toString(matrix));
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            for (int[] matrix : player2Matrix) {
                System.out.println(Arrays.toString(matrix));
            }
        }
    }

    private void placeBoat() {
        // Implement boat placement logic here
        System.out.println(players[currentPlayer - 1] + " placed " + boats[currentBoatIndex] + " at: " + selectedRow + ", " + selectedCol + " " + (isHorizontal ? "Horizontal" : "Vertical"));
        currentBoatIndex++;
        if (currentBoatIndex >= boats.length) {
            currentBoatIndex = 0;
            currentPlayer = 3 - currentPlayer; // Switch player
            setupBoats();
        }
    }

    public void placeBoatInMatrix(int[][] matrix, int selectedRow, int selectedCol, boolean isHorizontal, int boatSize) throws Exception {
        int lastRow = isHorizontal ? selectedRow : selectedRow + boatSize - 1;
        int lastCol = isHorizontal ? selectedCol + boatSize - 1 : selectedCol;
        if (selectedRow < 0 || selectedCol < 0 || lastRow > 9 || lastCol > 9) {
            throw new Exception("Invalid placement");
        }
        if (isHorizontal) {
            for (int i = 0; i < boatSize; i++) {
                if (matrix[selectedRow][selectedCol + i] == 1) {
                    throw new Exception("Invalid placement");
                }
            }
            for (int i = 0; i < boatSize; i++) {
                matrix[selectedRow][selectedCol + i] = 1;
            }
        } else {
            for (int i = 0; i < boatSize; i++) {
                if (matrix[selectedRow + i][selectedCol] == 1) {
                    throw new Exception("Invalid placement");
                }
            }
            for (int i = 0; i < boatSize; i++) {
                matrix[selectedRow + i][selectedCol] = 1;
            }
        }
    }

    private void finishNetPlacement() {
        session.buildFleet();
        waitingForOpponent = true;
        session.connection.send("READY");
        if (opponentReady) {
            startNetGame();
        }
    }

    private void startNetGame() {
        game.setScreen(new NetGameScreen(game, session));
    }

    /** @return false if the screen was changed and the frame should end. */
    private boolean pollNetwork() {
        String line;
        while ((line = session.connection.poll()) != null) {
            if ("READY".equals(line)) {
                opponentReady = true;
                if (waitingForOpponent) {
                    startNetGame();
                    return false; // keep remaining messages for the game screen
                }
            } else if ("LEAVE".equals(line)) {
                session.close();
                game.setScreen(new MenuScreen(game));
                return false;
            }
        }
        return true;
    }

    private void updateUi() {
        playerNameLabel.setText(players[currentPlayer - 1]);
        boatInfoLabel.setText(waitingForOpponent ? "Waiting for\nopponent..." : "Boat: " + boats[currentBoatIndex]);
        orientationLabel.setText("Orientation: " + (isHorizontal ? "Horizontal" : "Vertical"));
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


        for (Image boatImage : boatImages) {
            stage.addActor(boatImage);
        }

        // Create and position player name label
        playerNameLabel = new Label(players[currentPlayer - 1], skin, "title");
        playerNameLabel.setPosition(10 * GameConfig.CELL_SIZE, GameConfig.HUD_HEIGHT - GameConfig.CELL_SIZE - 10);
        stage.addActor(playerNameLabel);


        // Create and position boat info label
        boatInfoLabel = new Label("Boat:\n" + boats[currentBoatIndex], skin);
        boatInfoLabel.setPosition(20, GameConfig.HUD_HEIGHT - 80);
        stage.addActor(boatInfoLabel);

        // Create and position orientation label
        orientationLabel = new Label("Orientation:\n" + (isHorizontal ? "Horizontal" : "Vertical"), skin);
        orientationLabel.setPosition(20, GameConfig.HUD_HEIGHT - 120);
        stage.addActor(orientationLabel);
    }
}
