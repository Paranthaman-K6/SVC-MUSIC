package dev.local.vcmusic.gui;

import dev.local.vcmusic.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.function.Consumer;

public class TrackList extends ContainerObjectSelectionList<TrackEntry> {

    private Consumer<Track> onPick;

    public TrackList(int width, int height, int top, int itemHeight) {
        super(Minecraft.getInstance(), width, height, top, itemHeight);
    }

    public void onPick(Consumer<Track> onPick) {
        this.onPick = onPick;
    }

    public void setTracks(List<Track> tracks) {
        replaceEntries(tracks.stream().map(TrackEntry::new).toList());
    }

    /** Rows span the whole column; the default 220 would stick out of the panel. */
    @Override
    public int getRowWidth() {
        return getWidth();
    }

    public Track selectedTrack() {
        TrackEntry selected = getSelected();
        return selected == null ? null : selected.track();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        TrackEntry entry = getEntryAtPosition(event.x(), event.y());
        if (entry == null) {
            return false;
        }
        setSelected(entry);
        if (onPick != null) {
            onPick.accept(entry.track());
        }
        return true;
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor graphics) {
    }
}
