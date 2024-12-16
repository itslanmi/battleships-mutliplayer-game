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
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;

public class SettingsScreen extends ScreenAdapter {

    private static final String PREFS_NAME = "settingsPreferences";
    private static final String MAIN_MENU_MUSIC_KEY = "mainMenuMusic";
    private static final String GAMEPLAY_MUSIC_KEY = "gameplayMusic";
    private static final String MOVE_TIME_KEY = "moveTime";
    private static final String MULTIPLAYER_KEY = "multiplayer";

    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    private CheckBox mainMenuMusicCheckbox;
    private CheckBox gameplayMusicCheckbox;
    private SelectBox<String> moveTimeSelectBox;
    private CheckBox multiplayerCheckbox;

    public SettingsScreen(MomcilovicBattleshipGame game) {
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

        loadPreferences();
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

        Label titleLabel = new Label("Settings", skin, "title");
        table.add(titleLabel).colspan(2).center().padBottom(20).row();

        mainMenuMusicCheckbox = new CheckBox(" Main Menu Music", skin);
        table.add(mainMenuMusicCheckbox).left().padBottom(15).row();

        gameplayMusicCheckbox = new CheckBox(" Gameplay Music", skin);
        table.add(gameplayMusicCheckbox).left().padBottom(15).row();

        Label moveTimeLabel = new Label("Move Time:", skin);
        table.add(moveTimeLabel).left().padBottom(15).row();

        moveTimeSelectBox = new SelectBox<>(skin);
        moveTimeSelectBox.setItems("HARD (10s)", "MEDIUM (30s)", "EASY (60s)");
        table.add(moveTimeSelectBox).fillX().padBottom(15).row();

        multiplayerCheckbox = new CheckBox(" Multiplayer (LAN)", skin, "switch");
        table.add(multiplayerCheckbox).left().padBottom(15).row();

        TextButton backButton = new TextButton("Back", skin, "round");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                savePreferences();
                game.setScreen(new MenuScreen(game));
            }
        });
        table.add(backButton).colspan(2).center().padTop(20);

        table.center();
        table.setFillParent(true);
        table.pack();

        return table;
    }

    private void savePreferences() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putBoolean(MAIN_MENU_MUSIC_KEY, mainMenuMusicCheckbox.isChecked());
        prefs.putBoolean(GAMEPLAY_MUSIC_KEY, gameplayMusicCheckbox.isChecked());
        prefs.putString(MOVE_TIME_KEY, moveTimeSelectBox.getSelected());
        prefs.putBoolean(MULTIPLAYER_KEY, multiplayerCheckbox.isChecked());
        prefs.flush();
    }

    private void loadPreferences() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        mainMenuMusicCheckbox.setChecked(prefs.getBoolean(MAIN_MENU_MUSIC_KEY, true));
        gameplayMusicCheckbox.setChecked(prefs.getBoolean(GAMEPLAY_MUSIC_KEY, true));
        moveTimeSelectBox.setSelected(prefs.getString(MOVE_TIME_KEY, "30 seconds"));
        multiplayerCheckbox.setChecked(prefs.getBoolean(MULTIPLAYER_KEY, false));
    }
}
