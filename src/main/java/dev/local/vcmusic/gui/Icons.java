package dev.local.vcmusic.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** One pixel-art sprite sheet, so every control uses the same icon style. */
public final class Icons {

    public static final int NOTE = 0;
    public static final int SEARCH = 1;
    public static final int SAVE = 2;
    public static final int REMOVE = 3;
    public static final int PAUSE = 4;
    public static final int PLAY = 5;
    public static final int NEXT = 6;
    public static final int STOP = 7;
    public static final int VOLUME = 8;
    public static final int MIC = 9;
    public static final int QUEUE = 10;
    public static final int LINK = 11;
    public static final int FOLDER = 12;
    public static final int LIST = 13;
    public static final int ART = 14;

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("vcmusic", "textures/gui/icons.png");
    private static final int CELL = 32;

    private Icons() {
    }

    public static void draw(GuiGraphicsExtractor graphics, int icon, int x, int y) {
        draw(graphics, icon, x, y, 10);
    }

    public static void draw(GuiGraphicsExtractor graphics, int icon, int x, int y, int size) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y,
                icon * CELL, 0, size, size, CELL * 16, CELL);
    }
}
