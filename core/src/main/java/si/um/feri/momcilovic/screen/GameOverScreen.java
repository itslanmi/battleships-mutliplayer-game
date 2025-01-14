package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;

public class GameOverScreen extends ScreenAdapter {

    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    private String[] players;
    private float[] accuracies;
    private float[] scores;

    public GameOverScreen(MomcilovicBattleshipGame game, String[] players, float[] accuracies, float[] scores) {
        this.game = game;
        this.players = players;
        this.accuracies = accuracies;
        this.scores = scores;
        assetManager = game.getAssetManager();
    }

    @Override
    public void show() {
        viewport = new FillViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        game.getGameManager().saveResult(players[0], (int) scores[0], players[1], (int) scores[1], true);

        stage.addActor(createUi());
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 0f);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    private Actor createUi() {
        Table table = new Table();
        table.defaults().pad(20);

        TextureRegionDrawable backgroundRegion = new TextureRegionDrawable(gameplayAtlas.findRegion(RegionNames.BACKGROUND));
        table.setBackground(backgroundRegion);


        int winnerIndex = scores[0] > scores[1] ? 0 : 1;


        Label player1NameLabel = new Label(players[0], skin, "title");
        Label player1AccuracyLabel = new Label("Accuracy: " + Math.round(accuracies[0]) + "%", skin);
        Label player1ScoreLabel = new Label("Score: " + scores[0], skin);


        Label player2NameLabel = new Label(players[1], skin, "title");
        Label player2AccuracyLabel = new Label("Accuracy: " + Math.round(accuracies[1]) + "%", skin);
        Label player2ScoreLabel = new Label("Score: " + scores[1], skin);


        Label winnerLabel = new Label("Winner", skin, "title");
        Label winnerScoreLabel = new Label("Name: " + players[winnerIndex], skin);


        Table player1Table = new Table();
        player1Table.add(player1NameLabel).row();
        player1Table.add(player1AccuracyLabel).row();
        player1Table.add(player1ScoreLabel).row();


        Table player2Table = new Table();
        player2Table.add(player2NameLabel).row();
        player2Table.add(player2AccuracyLabel).row();
        player2Table.add(player2ScoreLabel).row();


        Table winnerTable = new Table();
        winnerTable.add(winnerLabel).row();
        winnerTable.add(winnerScoreLabel).row();


        TextButton leaderboardButton = new TextButton("Leaderboard", skin, "round");
        TextButton mainMenuButton = new TextButton("Main Menu", skin, "round");

        leaderboardButton.addListener(event -> {
            if (event.isHandled()) {
                game.setScreen(new LeaderboardScreen(game));
                return true;
            }
            return false;
        });

        mainMenuButton.addListener(event -> {
            if (event.isHandled()) {
                game.setScreen(new MenuScreen(game));
                return true;
            }
            return false;
        });

        table.add(player1Table).expandX().fillX();
        table.add(player2Table).expandX().fillX();
        table.row();
        table.add(winnerTable).colspan(2).center();
        table.row();
        table.add(leaderboardButton).colspan(2).center().padTop(20);
        table.row();
        table.add(mainMenuButton).colspan(2).center().padTop(10);

        table.setFillParent(true);
        table.pack();

        return table;
    }
}
