package si.um.feri.momcilovic.lwjgl3;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class AssetPacker {

    private static final boolean DRAW_DEBUG_OUTLINE = false;

    private static final String RAW_ASSETS_PATH = "lwjgl3";
    private static final String ASSETS_PATH = "assets";

    public static void main(String[] args) {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.debug = DRAW_DEBUG_OUTLINE;

        TexturePacker.process(settings,
            RAW_ASSETS_PATH + "/assets-raw",   // the directory containing individual images to be packed
            ASSETS_PATH + "/gameplay",   // the directory where the pack file will be written
            "gameplay"   // the name of the pack file / atlas name
        );

//        TexturePacker.process(settings,
//            RAW_ASSETS_PATH + "/skin",
//            ASSETS_PATH + "/ui",
//            "uiskin"
//        );
    }
}
