package dev.local.vcmusic.gui;

import dev.local.vcmusic.Library;
import dev.local.vcmusic.MusicConfig;
import dev.local.vcmusic.MusicPlayer;
import dev.local.vcmusic.Track;
import dev.local.vcmusic.YtDlp;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class MusicScreen extends Screen {

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 306;
    private static final int COLUMN = 194;
    private static final int TEXT = 0xFFE9F0F7;
    private static final int MUTED = 0xFF9AA7B8;
    private static final int ACCENT = 0xFF57D9FF;
    private static final int GOOD = 0xFF6EE7A0;
    private static final int WARN = 0xFFFFC857;
    private static final int BAD = 0xFFFF6B6B;
    private static final int PANEL_BG = 0xF0181E27;
    private static final int HEADER_BG = 0xF0242E3D;
    private static final int CARD_BG = 0xFF121A24;
    private static final int BORDER = 0xFF46566E;
    private static final int TRACK = 0xFF2C3948;

    private static final ItemStack NOTE = new ItemStack(Items.NOTE_BLOCK);
    private static final ItemStack DISC = new ItemStack(Items.MUSIC_DISC_CAT);
    private static final ItemStack QUEUE_ICON = new ItemStack(Items.HOPPER);

    private int left;
    private int top;
    private int lx;
    private int rx;

    private EditBox search;
    private TrackList trackList;
    private TrackList queueList;
    private Button modeButton;
    private Button pauseButton;

    private boolean showSaved;
    private String status = "";
    private List<Track> results = List.of();
    private long lastQueueStamp = -1;

    public MusicScreen() {
        super(Component.literal("Voice Chat Music"));
    }

    @Override
    protected void init() {
        clearWidgets();
        lastQueueStamp = -1;
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        lx = left + 10;
        rx = lx + COLUMN + 12;

        addRenderableWidget(tip(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + PANEL_WIDTH - 24, top + 4, 16, 12).build(), "Close the panel (K)"));

        search = new EditBox(font, lx, top + 24, 130, 18, Component.literal("Search"));
        search.setHint(Component.literal("YouTube link or song name..."));
        search.setMaxLength(300);
        addRenderableWidget(tip(search, "Paste a YouTube song or playlist link, or type a song name"));

        addRenderableWidget(tip(Button.builder(Component.literal("Search"), b -> search())
                .bounds(lx + 134, top + 24, 60, 18)
                .build(), "Look up the link or name; playlists appear as a list of songs"));

        trackList = new TrackList(COLUMN, PANEL_HEIGHT - 54 - 34, top + 54, 14);
        trackList.onPick(track -> {
            if (showSaved) {
                MusicPlayer.INSTANCE.play(track);
            } else {
                MusicPlayer.INSTANCE.enqueue(track);
            }
        });
        addRenderableWidget(tip(trackList,
                "Click a song to add it to the queue. In Saved mode it plays immediately"));

        modeButton = tip(Button.builder(modeLabel(), b -> {
            showSaved = !showSaved;
            refreshTrackList();
            b.setMessage(modeLabel());
        }).bounds(lx, top + PANEL_HEIGHT - 26, 62, 18).build(),
                "Switch this list between search results and your saved songs");
        addRenderableWidget(modeButton);

        addRenderableWidget(tip(Button.builder(Component.literal("Save"), b -> saveCurrent())
                .bounds(lx + 66, top + PANEL_HEIGHT - 26, 62, 18)
                .build(), "Save the selected song, the song playing, or a link typed above"));

        addRenderableWidget(tip(Button.builder(Component.literal("Remove"), b -> removeSelected())
                .bounds(lx + 132, top + PANEL_HEIGHT - 26, 62, 18)
                .build(), "Delete the selected song from your saved list"));

        pauseButton = tip(Button.builder(Component.literal("Pause"), b -> MusicPlayer.INSTANCE.togglePause())
                .bounds(rx, top + 80, 62, 18)
                .build(), "Pause or resume the music; it keeps its place in the song");
        addRenderableWidget(pauseButton);
        addRenderableWidget(tip(Button.builder(Component.literal("Next"), b -> MusicPlayer.INSTANCE.next())
                .bounds(rx + 66, top + 80, 62, 18)
                .build(), "Skip to the next song in the queue"));
        addRenderableWidget(tip(Button.builder(Component.literal("Stop"), b -> MusicPlayer.INSTANCE.stop())
                .bounds(rx + 132, top + 80, 62, 18)
                .build(), "Stop the music and clear the queue"));

        addRenderableWidget(tip(new ValueSlider(rx, top + 106, COLUMN, 18, "Volume", MusicConfig.INSTANCE.volume,
                value -> MusicConfig.INSTANCE.volume = (float) value),
                "How loud other players hear the music"));

        addRenderableWidget(tip(Button.builder(duckLabel(), b -> {
            MusicConfig.INSTANCE.duck = !MusicConfig.INSTANCE.duck;
            b.setMessage(duckLabel());
        }).bounds(rx, top + 128, COLUMN, 18).build(),
                "Turn the music down while you talk so your voice stays clear"));

        addRenderableWidget(tip(new ValueSlider(rx, top + 150, COLUMN, 18, "Monitor", MusicConfig.INSTANCE.localVolume,
                value -> MusicConfig.INSTANCE.localVolume = (float) value),
                "How loud you hear the music yourself"));

        addRenderableWidget(tip(Button.builder(Component.literal("Add all"), b -> addAll())
                .bounds(rx, top + 172, COLUMN, 18)
                .build(), "Queue every song in the current list, as a playlist"));

        queueList = new TrackList(COLUMN, PANEL_HEIGHT - 212 - 10, top + 212, 14);
        addRenderableWidget(tip(queueList, "Songs waiting to play, in order"));

        refreshTrackList();
        refreshQueue();
    }

    private static <T extends AbstractWidget> T tip(T widget, String text) {
        widget.setTooltip(Tooltip.create(Component.literal(text)));
        return widget;
    }

    private Component modeLabel() {
        return Component.literal(showSaved
                ? "Saved " + Library.INSTANCE.songs.size()
                : "Results " + results.size());
    }

    private Component duckLabel() {
        return Component.literal(MusicConfig.INSTANCE.duck ? "Duck while speaking" : "Ducking off");
    }

    private void refreshTrackList() {
        trackList.setTracks(showSaved ? List.copyOf(Library.INSTANCE.songs) : results);
    }

    private void refreshQueue() {
        List<Track> queue = MusicPlayer.INSTANCE.queueSnapshot();
        long stamp = queue.size() * 31L + (queue.isEmpty() ? 0 : queue.get(0).url().hashCode());
        if (stamp != lastQueueStamp) {
            lastQueueStamp = stamp;
            queueList.setTracks(queue);
        }
    }

    private void search() {
        String query = search.getValue().trim();
        if (query.isEmpty()) {
            return;
        }
        status = "Searching...";
        CompletableFuture.supplyAsync(() -> {
            try {
                return YtDlp.search(MusicConfig.INSTANCE, query, 10);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).whenComplete((tracks, error) -> minecraft.execute(() -> {
            if (error != null) {
                status = "Search failed: " + reason(error);
                return;
            }
            results = tracks;
            showSaved = false;
            modeButton.setMessage(modeLabel());
            refreshTrackList();
            status = looksLikeLink(query) && tracks.size() > 1
                    ? "Playlist: " + tracks.size() + " songs"
                    : (tracks.isEmpty() ? "No results" : tracks.size() + " result(s)");
        }));
    }

    private static boolean looksLikeLink(String text) {
        String s = text.trim().toLowerCase();
        return s.startsWith("http://") || s.startsWith("https://") || s.startsWith("www.");
    }

    /** Innermost message, so the panel shows yt-dlp's reason instead of a wrapper. */
    private static String reason(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }

    private void saveCurrent() {
        Track track = trackList.selectedTrack();
        if (track == null) {
            track = MusicPlayer.INSTANCE.current();
        }
        if (track == null && looksLikeLink(search.getValue())) {
            String url = search.getValue().trim();
            track = new Track(url, url, 0);
        }
        if (track == null) {
            status = "Nothing selected - pick a song or paste a link";
            return;
        }
        boolean added = Library.INSTANCE.add(track);
        status = added ? "Saved: " + track.title() : "Already saved";
        // Show the saved list immediately, otherwise Save looks like it did nothing.
        showSaved = true;
        modeButton.setMessage(modeLabel());
        refreshTrackList();
    }

    private void addAll() {
        List<Track> source = showSaved ? Library.INSTANCE.songs : results;
        if (source.isEmpty()) {
            status = "Nothing to queue";
            return;
        }
        for (Track track : source) {
            MusicPlayer.INSTANCE.enqueue(track);
        }
        status = "Queued " + source.size() + " song(s)";
    }

    private void removeSelected() {
        Track selected = trackList.selectedTrack();
        if (selected == null) {
            return;
        }
        Library.INSTANCE.remove(selected);
        refreshTrackList();
        status = "Removed: " + selected.title();
    }

    @Override
    public void tick() {
        super.tick();
        refreshQueue();
        pauseButton.setMessage(Component.literal(MusicPlayer.INSTANCE.paused() ? "Resume" : "Pause"));
        modeButton.setMessage(modeLabel());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        graphics.fill(left - 6, top - 6, right + 6, bottom + 6, 0xB0000000);
        graphics.fill(left - 2, top - 2, right + 2, bottom + 2, BORDER);
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, PANEL_BG);

        graphics.fill(left, top, right, top + 20, HEADER_BG);
        graphics.fill(left, top, left + 3, top + 20, ACCENT);
        icon(graphics, NOTE, left + 8, top + 4, 12);
        graphics.text(font, title, left + 24, top + 6, TEXT, false);
        graphics.text(font, "K to close", right - 110, top + 7, MUTED, false);

        graphics.text(font, showSaved ? "SAVED SONGS" : "SONGS", lx, top + 43, ACCENT, false);
        graphics.text(font, "PLAYER", rx, top + 26, ACCENT, false);
        if (!status.isEmpty()) {
            String shown = fit(status, COLUMN - 52);
            graphics.text(font, shown, right - 10 - font.width(shown), top + 26, MUTED, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        icon(graphics, NOTE, rx + 3, top + 109);
        icon(graphics, NOTE, rx + 3, top + 131);
        icon(graphics, NOTE, rx + 3, top + 153);
        icon(graphics, QUEUE_ICON, rx + 3, top + 175);

        int card = top + 42;
        graphics.fill(rx - 1, card - 1, rx + COLUMN + 1, card + 35, BORDER);
        graphics.fill(rx, card, rx + COLUMN, card + 34, CARD_BG);

        Track current = MusicPlayer.INSTANCE.current();
        AlbumArt.Art art = current == null ? null : AlbumArt.get(current.thumbnail());
        if (art != null) {
            AlbumArt.draw(graphics, art, rx + 5, card + 4, 16);
        } else {
            icon(graphics, DISC, rx + 6, card + 5, 14);
        }
        String now = fit(current == null ? "Nothing selected" : current.title(), COLUMN - 34);
        graphics.text(font, now, rx + 24, card + 5, current == null ? MUTED : TEXT, false);
        if (current != null && mouseX >= rx + 24 && mouseX < rx + COLUMN && mouseY >= card + 5 && mouseY < card + 16) {
            graphics.setTooltipForNextFrame(Component.literal(current.display()), mouseX, mouseY);
        }

        String state;
        String detail = "";
        int color = MUTED;
        if (current == null) {
            String error = MusicPlayer.INSTANCE.lastError();
            state = error.isEmpty() ? "Idle" : "Failed: " + error;
            if (!error.isEmpty()) {
                color = BAD;
                detail = error;
            }
        } else if (MusicPlayer.INSTANCE.paused()) {
            state = "Paused";
            color = WARN;
        } else if (MusicPlayer.INSTANCE.voiceChatSilent()) {
            state = "Enable voice chat to play music";
            color = WARN;
        } else if (MusicPlayer.INSTANCE.buffering()) {
            state = "Buffering...";
            color = ACCENT;
        } else {
            state = "Playing";
            color = GOOD;
        }
        String fullState = state;
        state = fit(state, COLUMN - 78);
        graphics.text(font, state, rx + 24, card + 17, color, false);
        if (mouseX >= rx + 24 && mouseX < rx + COLUMN && mouseY >= card + 17 && mouseY < card + 28) {
            graphics.setTooltipForNextFrame(
                    Component.literal(detail.isEmpty() ? stateTooltip(fullState, current != null) : detail),
                    mouseX, mouseY);
        }

        long position = MusicPlayer.INSTANCE.positionMs();
        int duration = current == null ? 0 : current.durationSeconds();
        String time = clock(position) + " / " + (duration > 0 ? clock(duration * 1000L) : "--:--");
        graphics.text(font, time, right - 8 - font.width(time), card + 17, MUTED, false);
        graphics.fill(rx + 6, card + 28, rx + COLUMN - 6, card + 31, TRACK);
        if (duration > 0) {
            int width = (int) Math.min(COLUMN - 12, (long) (COLUMN - 12) * position / (duration * 1000L));
            graphics.fill(rx + 6, card + 28, rx + 6 + width, card + 31, ACCENT);
        }

        graphics.text(font, "QUEUE (" + MusicPlayer.INSTANCE.queueSnapshot().size() + ")",
                rx, top + 200, ACCENT, false);
        graphics.fill(rx, top + 210, rx + COLUMN, top + 211, BORDER);
    }

    private String fit(String text, int max) {
        return font.width(text) > max ? font.plainSubstrByWidth(text, max - 6) + "..." : text;
    }

    private static String clock(long millis) {
        long seconds = millis / 1000;
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    private void icon(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        icon(graphics, stack, x, y, 10);
    }

    private void icon(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int size) {
        if (minecraft.level != null && minecraft.player != null) {
            graphics.item(stack, x, y, size);
        }
    }

    private static String stateTooltip(String state, boolean playing) {
        if (state.startsWith("Enable voice chat")) {
            return "Voice chat is muted, disabled or not connected. Unmute it and enable it in the voice chat menu";
        }
        if (state.startsWith("Buffering")) {
            return "yt-dlp is fetching the audio, this can take a few seconds";
        }
        if (state.startsWith("Paused")) {
            return "Music is paused; press Resume to continue";
        }
        return playing ? "The song is playing to nearby players and in your headphones" : "Nothing is playing";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        MusicConfig.INSTANCE.save();
        Library.INSTANCE.save();
        super.onClose();
    }
}
