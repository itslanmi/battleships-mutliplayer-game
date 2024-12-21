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

import si.um.feri.momcilovic.GameManager;
import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;

public class GameScreen extends ScreenAdapter {

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
    private String[] players;

    private Image[] boatImages;
    private String[] boats = {RegionNames.GRID_BATTLESHIP, RegionNames.GRID_CRUISER, RegionNames.GRID_DESTROYER, RegionNames.GRID_SUBMARINE, RegionNames.GRID_PATROL};
    private int currentBoatIndex = 0;

    private int minRow = 2;
    private int maxRow = 11;

    private int minCol = 5;
    private int maxCol = 14;

    private Label playerNameLabel;
    private Label boatInfoLabel;
    private Label orientationLabel;

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

        boatImages = new Image[boats.length];
        for (int i = 0; i < boats.length; i++) {
            boatImages[i] = new Image(new TextureRegionDrawable(gameplayAtlas.findRegion(boats[i])));
            boatImages[i].setPosition( 0, -64);
        }
        boatImages[0].setPosition(5*64, 11*64);

        players = new String[] {gameManager.getGameResult(gameManager.getGameResults().size()-1).getPlayer(1),
            gameManager.getGameResult(gameManager.getGameResults().size()-1).getPlayer(2)};

        createUi();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 0f);

        handleInput();
        updateUi();
        stage.act(delta);
        stage.draw();
    }

    private void changeLimits(){
        if(isHorizontal){
            minRow = 2;
            maxRow = 11;
            minCol = 5;
            maxCol = 15 - (int) (boatImages[currentBoatIndex].getWidth()/64);
        }else {
            minRow = 2 + (int) (boatImages[currentBoatIndex].getWidth()/64);
            maxRow = 12;
            minCol = 5;
            maxCol = 14;
        }
    }

    private void handleInput() {
        changeLimits();
        float cellSize = GameConfig.CELL_SIZE;

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            if(boatImages[currentBoatIndex].getY() < maxRow*cellSize){
                boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), boatImages[currentBoatIndex].getY() + cellSize);
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            if(boatImages[currentBoatIndex].getY() > minRow*cellSize){
                boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), boatImages[currentBoatIndex].getY() - cellSize);
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            if (boatImages[currentBoatIndex].getX() > minCol*cellSize) {
                boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX() - cellSize, boatImages[currentBoatIndex].getY());
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            if (boatImages[currentBoatIndex].getX() < maxCol * cellSize) {
                boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX() + cellSize, boatImages[currentBoatIndex].getY());
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.R)){
            if(boatImages[currentBoatIndex].getRotation() == 0){
                boatImages[currentBoatIndex].rotateBy(-90);
                if(boatImages[currentBoatIndex].getY() - boatImages[currentBoatIndex].getImageWidth() < minRow*cellSize){
                    boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), minRow*cellSize+boatImages[currentBoatIndex].getImageWidth());
                }else {
                    boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), boatImages[currentBoatIndex].getY()+cellSize);
                }
            }else {
                boatImages[currentBoatIndex].rotateBy(90);
                if(boatImages[currentBoatIndex].getX() + boatImages[currentBoatIndex].getImageWidth() > maxCol*cellSize){
                    boatImages[currentBoatIndex].setPosition(maxCol*cellSize-boatImages[currentBoatIndex].getImageWidth()+cellSize, boatImages[currentBoatIndex].getY()-cellSize);
                } else {
                    boatImages[currentBoatIndex].setPosition(boatImages[currentBoatIndex].getX(), boatImages[currentBoatIndex].getY()-cellSize);
                }
            }
            isHorizontal = !isHorizontal;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            placeBoat();
            isHorizontal = true;
            changeLimits();
            boatImages[currentBoatIndex].setPosition(minCol*cellSize, maxRow*cellSize);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
            System.out.println("MinRow: " + minRow + " MaxRow: " + maxRow + " MinCol: " + minCol + " MaxCol: " + maxCol);
        }

    }

    private void placeBoat() {
        // Implement boat placement logic here
        System.out.println(players[currentPlayer - 1] + " placed " + boats[currentBoatIndex] + " at: " + selectedRow + ", " + selectedCol + " " + (isHorizontal ? "Horizontal" : "Vertical"));
        currentBoatIndex++;
        if (currentBoatIndex >= boats.length) {
            currentBoatIndex = 0;
            currentPlayer = 3 - currentPlayer; // Switch player
        }
    }

    private void updateUi() {
        playerNameLabel.setText(players[currentPlayer - 1]);
        boatInfoLabel.setText("Boat: " + boats[currentBoatIndex]);
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

        for(Image boatImage : boatImages){
            stage.addActor(boatImage);
        }

        // Create and position player name label
        playerNameLabel = new Label(players[currentPlayer - 1], skin, "title");
        playerNameLabel.setPosition(10 * 64, GameConfig.HUD_HEIGHT - 64);
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
