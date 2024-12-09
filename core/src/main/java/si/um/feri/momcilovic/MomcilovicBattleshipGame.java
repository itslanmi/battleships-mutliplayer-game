package si.um.feri.momcilovic;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import si.um.feri.momcilovic.assets.AssetDescriptors;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MomcilovicBattleshipGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;

    private AssetManager assetManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        assetManager = new AssetManager();
        assetManager.load(AssetDescriptors.GAMEPLAY);
        assetManager.load(AssetDescriptors.UI_FONT);
        assetManager.load(AssetDescriptors.UI_SOUND_DROP);
        assetManager.load(AssetDescriptors.UI_SOUND_EXPLOSION);
        assetManager.load(AssetDescriptors.UI_SOUND_MISS);
        assetManager.finishLoading();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
    }
}
