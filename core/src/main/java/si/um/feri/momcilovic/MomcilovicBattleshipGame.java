package si.um.feri.momcilovic;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ScreenUtils;

import si.um.feri.momcilovic.assets.AssetDescriptors;
import si.um.feri.momcilovic.assets.AssetPaths;
import si.um.feri.momcilovic.screen.IntroScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MomcilovicBattleshipGame extends Game {
    private SpriteBatch batch;

    private Sound explosionSound;

    private AssetManager assetManager;

    private GameManager gameManager;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        batch = new SpriteBatch();
        gameManager = new GameManager();

        assetManager = new AssetManager();
        assetManager.getLogger().setLevel(Logger.DEBUG);
        assetManager.load(AssetDescriptors.GAMEPLAY);
        assetManager.load(AssetDescriptors.UI_FONT);
        assetManager.load(AssetDescriptors.UI_SOUND_DROP);
        assetManager.load(AssetDescriptors.UI_SOUND_EXPLOSION);
        assetManager.load(AssetDescriptors.UI_SOUND_MISS);
        assetManager.load(AssetDescriptors.UI_SKIN);
        assetManager.finishLoading();

        explosionSound = assetManager.get(AssetPaths.UI_SOUND_EXPLOSION, Sound.class);

        gameManager.saveResult("test", 1000,"test2",2000, false);
        gameManager.saveResult("aas", 1111,"asd",2222, false);

        setScreen(new IntroScreen(this));
    }


    @Override
    public void dispose() {
        assetManager.dispose();
        batch.dispose();
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public Sound getExplosionSound() {
        return explosionSound;
    }
}
