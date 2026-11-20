package dev.local.vcmusic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class YtDlp {

    private YtDlp() {
    }

    /**
     * Accepts a search term, a YouTube song URL or a YouTube playlist URL. A plain term is searched;
     * a URL goes straight to yt-dlp, which returns one JSON object for a song and an entries array
     * for a playlist.
     */
    public static List<Track> search(MusicConfig config, String query, int limit) throws IOException {
        boolean link = query.startsWith("http://") || query.startsWith("https://") || query.startsWith("www.");
        List<String> command = new ArrayList<>(List.of(MusicConfig.resolve(config.ytdlp),
                "--flat-playlist", "--no-warnings", "--no-progress", "-J"));
        if (link) {
            command.add("--yes-playlist");
        }
        command.add(link ? query : "ytsearch" + limit + ":" + query);
        Process process = new ProcessBuilder(command).start();

        byte[] out;
        try {
            out = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()))
                    .get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            process.destroyForcibly();
            throw new IOException("yt-dlp search timed out");
        } catch (Exception e) {
            process.destroyForcibly();
            throw new IOException("yt-dlp search failed: " + e.getMessage(), e);
        }

        JsonObject root;
        try {
            root = JsonParser.parseString(new String(out)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("yt-dlp returned no usable data", e);
        }
        JsonArray entries = root.getAsJsonArray("entries");
        List<Track> tracks = new ArrayList<>();
        if (entries == null) {
            Track track = track(root);
            if (track != null) {
                tracks.add(track);
            }
        } else {
            for (JsonElement element : entries) {
                Track track = track(element.getAsJsonObject());
                if (track != null) {
                    tracks.add(track);
                }
            }
        }
        return tracks;
    }

    private static Track track(JsonObject entry) {
        String id = string(entry, "id");
        String url = string(entry, "url");
        if (url == null && id != null) {
            url = "https://www.youtube.com/watch?v=" + id;
        }
        if (url == null) {
            return null;
        }
        String title = string(entry, "title");
        int duration = 0;
        JsonElement d = entry.get("duration");
        if (d != null && d.isJsonPrimitive()) {
            duration = (int) d.getAsDouble();
        }
        return new Track(title == null ? url : title, url, duration);
    }

    private static String string(JsonObject object, String key) {
        JsonElement e = object.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static byte[] readAll(InputStream in) {
        try (in) {
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
