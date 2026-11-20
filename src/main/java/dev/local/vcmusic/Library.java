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

public final class Library {

    public static Library INSTANCE = new Library();

    public final List<Track> songs = new ArrayList<>();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("vcmusic/library.json");
    }

    public static Library load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                List<Track> list = new Gson().fromJson(r, new TypeToken<List<Track>>() {
                }.getType());
                if (list != null) {
                    INSTANCE.songs.clear();
                    INSTANCE.songs.addAll(list);
                }
            } catch (Exception e) {
                System.err.println("[vcmusic] Could not read library: " + e);
            }
        }
        return INSTANCE;
    }

    public void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(songs, w);
            }
        } catch (Exception e) {
            System.err.println("[vcmusic] Could not save library: " + e);
        }
    }

    public boolean add(Track track) {
        if (songs.stream().anyMatch(t -> t.url().equals(track.url()))) {
            return false;
        }
        songs.add(track);
        save();
        return true;
    }

    public void remove(Track track) {
        songs.removeIf(t -> t.url().equals(track.url()));
        save();
    }
}
