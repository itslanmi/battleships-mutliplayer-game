package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import si.um.feri.momcilovic.GameManager;
import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;

public class LeaderboardScreen extends ScreenAdapter {

    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;
    private final GameManager gameManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    public LeaderboardScreen(MomcilovicBattleshipGame game) {
        this.game = game;
        this.assetManager = game.getAssetManager();
        this.gameManager = game.getGameManager();
    }

    @Override
    public void show() {
        viewport = new FillViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

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
        table.defaults().pad(20).width(600);

        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.BACKGROUND);
        table.setBackground(new TextureRegionDrawable(backgroundRegion));

        TextButton backButton = new TextButton("Back", skin, "round");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        Table buttonTable = new Table();
        buttonTable.defaults().padLeft(30).padRight(30);

        Label titleLabel = new Label("Leaderboard", skin, "title");
        buttonTable.add(titleLabel).colspan(3).center().padBottom(20).row();

        Table leaderboardTable = new Table();
        leaderboardTable.defaults().pad(10);

        List<GameManager.GameResult> gameResults = gameManager.getGameResults();
        List<PlayerScore> playerScores = new ArrayList<>();
        for (GameManager.GameResult result : gameResults) {
            playerScores.add(new PlayerScore(result.getPlayer(1), result.getScore(1)));
            playerScores.add(new PlayerScore(result.getPlayer(2), result.getScore(2)));
        }

        playerScores.sort(Comparator.comparingInt(PlayerScore::getScore).reversed());

        for (int i = 0; i < playerScores.size(); i++) {
            Label rankLabel = new Label((i + 1) + ".", skin, "default");
            Label playerLabel = new Label(playerScores.get(i).getPlayer(), skin, "default");
            Label scoreLabel = new Label(Integer.toString(playerScores.get(i).getScore()), skin, "default");

            leaderboardTable.add(rankLabel).left().padRight(10);
            leaderboardTable.add(playerLabel).expandX().fillX().padRight(10);
            leaderboardTable.add(scoreLabel).expandX().fillX().row();
        }

        ScrollPane scrollPane = new ScrollPane(leaderboardTable, skin);
        scrollPane.setScrollingDisabled(true, false);
        buttonTable.add(scrollPane).expand().fill().row();

        buttonTable.add(backButton).padBottom(15).row();

        buttonTable.center();

        table.add(buttonTable);
        table.center();
        table.setFillParent(true);
        table.pack();

        return table;
    }

    private static class PlayerScore {
        private final String player;
        private final int score;

        public PlayerScore(String player, int score) {
            this.player = player;
            this.score = score;
        }

        public String getPlayer() {
            return player;
        }

        public int getScore() {
            return score;
        }
    }
}
