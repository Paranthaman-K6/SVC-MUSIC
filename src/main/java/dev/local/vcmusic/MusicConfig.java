package dev.local.vcmusic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * User settings. Paths are resolved from the command name, so "yt-dlp" works when it is on PATH.
 */
public final class MusicConfig {

    public static MusicConfig INSTANCE = new MusicConfig();

    public String ytdlp = "yt-dlp";
    public String ffmpeg = "ffmpeg";
    /** Volume of the music in the stream other players hear (0..1). */
    public float volume = 0.6f;
    /** Lower the music while you speak so your voice stays intelligible. Never mutes it. */
    public boolean duck = true;
    /** Music level while speaking (0..1). */
    public float duckLevel = 0.35f;
    /** Volume of the local monitor copy only (0..1). */
    public float localVolume = 1.0f;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("vcmusic/config.json");
    }

    /**
     * Absolute path for a bare command name. A game started from a desktop icon often has a short
     * PATH that misses ~/.local/bin, which is where yt-dlp usually lives.
     */
    public static String resolve(String command) {
        if (command.contains("/")) {
            return command;
        }
        String home = System.getProperty("user.home", "");
        String search = (System.getenv("PATH") == null ? "" : System.getenv("PATH"))
                + ":" + home + "/.local/bin:/usr/local/bin:/usr/bin";
        for (String dir : search.split(":")) {
            if (dir.isBlank()) {
                continue;
            }
            Path candidate = Path.of(dir, command);
            if (Files.isExecutable(candidate)) {
                return candidate.toString();
            }
        }
        return command;
    }

    public static MusicConfig load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                MusicConfig c = new Gson().fromJson(r, MusicConfig.class);
                if (c != null) {
                    INSTANCE = c;
                }
            } catch (Exception e) {
                System.err.println("[vcmusic] Could not read config: " + e);
            }
        }
        return INSTANCE;
    }

    public void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(this, w);
            }
        } catch (IOException e) {
            System.err.println("[vcmusic] Could not save config: " + e);
        }
    }
}
