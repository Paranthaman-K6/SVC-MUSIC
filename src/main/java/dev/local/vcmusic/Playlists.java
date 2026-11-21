package dev.local.vcmusic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** All named playlists, stored next to the saved songs so they work on every server. */
public final class Playlists {

    public static Playlists INSTANCE = new Playlists();

    public final List<Playlist> lists = new ArrayList<>();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("vcmusic/playlists.json");
    }

    public static Playlists load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                List<Playlist> loaded = new Gson().fromJson(r, new TypeToken<List<Playlist>>() {
                }.getType());
                if (loaded != null) {
                    INSTANCE.lists.clear();
                    INSTANCE.lists.addAll(loaded);
                }
            } catch (Exception e) {
                System.err.println("[vcmusic] Could not read playlists: " + e);
            }
        }
        return INSTANCE;
    }

    public void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(lists, w);
            }
        } catch (Exception e) {
            System.err.println("[vcmusic] Could not save playlists: " + e);
        }
    }

    /** Case-insensitive, so "Rock" and "rock" are the same playlist. */
    public Playlist find(String name) {
        for (Playlist playlist : lists) {
            if (playlist.name().equalsIgnoreCase(name.trim())) {
                return playlist;
            }
        }
        return null;
    }

    /** Returns the existing playlist when the name is taken, so Create never duplicates one. */
    public Playlist create(String name) {
        Playlist existing = find(name);
        if (existing != null) {
            return existing;
        }
        Playlist playlist = new Playlist(name.trim(), new ArrayList<>());
        lists.add(playlist);
        save();
        return playlist;
    }

    public void delete(Playlist playlist) {
        lists.remove(playlist);
        save();
    }
}
