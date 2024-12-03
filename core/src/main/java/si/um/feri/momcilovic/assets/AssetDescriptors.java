package si.um.feri.momcilovic.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class AssetDescriptors {

    public static final AssetDescriptor<BitmapFont> UI_FONT =
        new AssetDescriptor<BitmapFont>(AssetPaths.UI_FONT, BitmapFont.class);

    public static final AssetDescriptor<Sound> UI_SOUND_DROP =
        new AssetDescriptor<Sound>(AssetPaths.UI_SOUND_DROP, Sound.class);

    public static final AssetDescriptor<Sound> UI_SOUND_EXPLOSION =
        new AssetDescriptor<Sound>(AssetPaths.UI_SOUND_EXPLOSION, Sound.class);

    public static final AssetDescriptor<Music> UI_MUSIC =
        new AssetDescriptor<Music>(AssetPaths.UI_MUSIC, Music.class);

    public static final AssetDescriptor<TextureAtlas> GAMEPLAY =
        new AssetDescriptor<TextureAtlas>(AssetPaths.GAMEPLAY, TextureAtlas.class);

    private AssetDescriptors() {
    }
}
