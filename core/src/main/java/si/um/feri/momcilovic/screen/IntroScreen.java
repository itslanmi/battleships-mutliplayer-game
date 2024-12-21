package si.um.feri.momcilovic.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


import si.um.feri.momcilovic.MomcilovicBattleshipGame;
import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.RegionNames;
import si.um.feri.momcilovic.config.GameConfig;

public class IntroScreen extends ScreenAdapter {

    public static final float INTRO_DURATION_IN_SEC = 10f;   // duration of the (intro) animation

    private final MomcilovicBattleshipGame game;
    private final AssetManager assetManager;

    private Viewport viewport;
    private TextureAtlas gameplayAtlas;

    private float duration = 0f;

    private Stage stage;

    public IntroScreen(MomcilovicBattleshipGame game) {
        this.game = game;
        assetManager = game.getAssetManager();
    }

    @Override
    public void show() {
        viewport = new FillViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        // load assets
        assetManager.load(AssetDescriptors.UI_FONT);
        assetManager.load(AssetDescriptors.UI_SKIN);
        assetManager.load(AssetDescriptors.GAMEPLAY);
        assetManager.load(AssetDescriptors.UI_SOUND_EXPLOSION);
        assetManager.finishLoading();   // blocks until all assets are loaded

        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        addBackground();
        animateShipsAndLogo();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 0f);

        duration += delta;

        // go to the MenuScreen after INTRO_DURATION_IN_SEC seconds
        if (duration > INTRO_DURATION_IN_SEC || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            game.setScreen(new MenuScreen(game));
        }

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

    private void addBackground() {
        Image background = new Image(gameplayAtlas.findRegion(RegionNames.BACKGROUND));
        background.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
        stage.addActor(background);
    }


    private void animateShipsAndLogo() {
        Image[] ships = new Image[7];

        String[] shipRegions = {
            RegionNames.SHIP_PATROL,
            RegionNames.SHIP_DESTROYER,
            RegionNames.SHIP_SUBMARINE,
            RegionNames.SHIP_BATTLESHIP,
            RegionNames.SHIP_CRUISER,
            RegionNames.SHIP_DESTROYER,
            RegionNames.SHIP_PATROL,
        };
        // Create ship actors
        for (int i = 0; i < ships.length; i++) {
            ships[i] = new Image(gameplayAtlas.findRegion(shipRegions[i])); // Example: use a destroyer texture
            ships[i].setPosition(-ships[i].getWidth(), viewport.getWorldHeight() / 2f + (i - 3) * 50);
            ships[i].setRotation(-90); // Rotate ships to face right
            stage.addActor(ships[i]);
        }

        // Define target positions for the formation (center-left screen)
        float formationCenterX = viewport.getWorldWidth() / 3f;
        float formationCenterY = viewport.getWorldHeight() / 2f;
        float formationSpacing = 50f;


        // Animate ships to form the formation
        for (int i = 0; i < ships.length; i++) {
            float targetX = formationCenterX - Math.abs(i - 3) * formationSpacing;
            float targetY = formationCenterY + (i - 3) * formationSpacing;

            ships[i].addAction(Actions.sequence(
                Actions.moveTo(targetX, targetY, 2f), // Move to formation
                Actions.delay(0.5f), // Wait for all ships to align
                Actions.run(() -> moveFormationAcrossScreen(ships)) // Trigger movement across the screen
            ));
        }
    }

    private void moveFormationAcrossScreen(Image[] ships) {
        for (Image ship : ships) {
            ship.addAction(Actions.sequence(
                Actions.moveBy(viewport.getWorldWidth(), 0, 2f),
                Actions.run(this::revealLogo)
            ));
        }
    }



    private void revealLogo() {
        stage.clear();
        addBackground();

        Image logo = new Image(gameplayAtlas.findRegion(RegionNames.BATTLESHIP_LOGO));
        logo.setPosition(viewport.getWorldWidth() / 2f - logo.getWidth() / 2f,
            viewport.getWorldHeight() / 2f - logo.getHeight() / 2f);
        logo.getColor().a = 0;
        stage.addActor(logo);


        logo.addAction(Actions.sequence(
            Actions.fadeIn(1f) // Fade in over 1 second
            //Actions.run(() -> game.getSoundManager().play("logo_reveal_sound")) // Play a sound if desired
        ));

    }


}
