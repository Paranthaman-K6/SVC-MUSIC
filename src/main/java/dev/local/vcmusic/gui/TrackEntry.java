package dev.local.vcmusic.gui;

import dev.local.vcmusic.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class TrackEntry extends ContainerObjectSelectionList.Entry<TrackEntry> {

    private static final ItemStack ICON = new ItemStack(Items.MUSIC_DISC_CAT);

    private final Track track;

    public TrackEntry(Track track) {
        this.track = track;
    }

    public Track track() {
        return track;
    }

    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
        int x = getContentX();
        int y = getContentY();
        int width = getContentWidth();
        int height = getContentHeight();
        if (hovered) {
            graphics.fill(x, y, x + width, y + height, 0x40FFFFFF);
            // Playlist rows carry a count in the title, so the song tooltip would be wrong.
            graphics.setTooltipForNextFrame(Component.literal(track.isPlaylist() ? track.title() : track.display()), mouseX, mouseY);
        }
        Minecraft minecraft = Minecraft.getInstance();
        String text = track.title();
        int max = width - 22;
        if (minecraft.font.width(text) > max) {
            text = minecraft.font.plainSubstrByWidth(text, max - 6) + "...";
        }
        AlbumArt.Art art = AlbumArt.get(track.thumbnail());
        if (art != null) {
            AlbumArt.draw(graphics, art, x + 1, y, 10);
        } else if (minecraft.level != null) {
            graphics.item(ICON, x + 1, y, 10);
        }
        graphics.text(minecraft.font, text, x + 14, y + (height - minecraft.font.lineHeight) / 2, 0xFFFFFFFF, false);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of();
    }
}
