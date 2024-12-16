package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;

public class PreGameScreen extends ScreenAdapter {

    private static final String PREFS_NAME = "settingsPreferences";
    private static final String MULTIPLAYER_KEY = "multiplayer";

    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    public PreGameScreen(MomcilovicBattleshipGame game) {
        this.game = game;
        assetManager = game.getAssetManager();
    }

    @Override
    public void show() {
        viewport = new FitViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
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
        table.defaults().pad(20);

        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.BACKGROUND);
        table.setBackground(new TextureRegionDrawable(backgroundRegion));

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        boolean isMultiplayer = prefs.getBoolean(MULTIPLAYER_KEY, false);

        Label titleLabel = new Label("Pre-Game Setup", skin, "title");
        table.add(titleLabel).colspan(2).center().padBottom(20).row();

        if (isMultiplayer) {
            Label playerNameLabel = new Label("Player Name:", skin);
            TextField playerNameField = new TextField("", skin);
            table.add(playerNameLabel).left().padBottom(15).row();
            table.add(playerNameField).fillX().padBottom(15).row();
        } else {
            Label player1NameLabel = new Label("Player 1 Name:", skin);
            TextField player1NameField = new TextField("", skin);
            Label player2NameLabel = new Label("Player 2 Name:", skin);
            TextField player2NameField = new TextField("", skin);
            table.add(player1NameLabel).left().padBottom(15).row();
            table.add(player1NameField).fillX().padBottom(15).row();
            table.add(player2NameLabel).left().padBottom(15).row();
            table.add(player2NameField).fillX().padBottom(15).row();
        }

        Table buttonTable = new Table();
        TextButton backButton = new TextButton("Back", skin, "round");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
        buttonTable.add(backButton).padRight(10);

        TextButton startButton = new TextButton("Start Game", skin, "round");
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // game.setScreen(new GameScreen(game));
            }
        });
        buttonTable.add(startButton).padLeft(10);

        table.add(buttonTable).colspan(2).center().padTop(20);

        table.center();
        table.setFillParent(true);
        table.pack();

        return table;
    }
}
