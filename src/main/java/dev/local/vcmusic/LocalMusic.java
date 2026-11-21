package dev.local.vcmusic;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** Reads a local audio file or folder typed into the search box. */
public final class LocalMusic {

    private static final Set<String> AUDIO = Set.of("mp3", "flac", "wav", "m4a", "ogg", "opus");
    /** Deep enough for Artist/Album/track, shallow enough not to walk an entire home folder. */
    private static final int DEPTH = 3;

    private LocalMusic() {
    }

    /** True when the text names something on disk, so it should not go to YouTube. */
    public static boolean isPath(String query) {
        return path(query) != null;
    }

    /**
     * Songs in a file or folder, or null when the text is not a path (a YouTube search).
     * Paths become file: URLs so {@link AudioStream} can tell them from yt-dlp links.
     */
    public static List<Track> scan(String query) throws IOException {
        Path path = path(query);
        if (path == null) {
            return null;
        }
        if (Files.isRegularFile(path)) {
            return List.of(track(path));
        }
        try (Stream<Path> files = Files.walk(path, DEPTH)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> AUDIO.contains(extension(p)))
                    .sorted()
                    .map(LocalMusic::track)
                    .toList();
        }
    }

    private static Path path(String query) {
        String text = query.trim();
        if (text.startsWith("~")) {
            text = System.getProperty("user.home", "") + text.substring(1);
        }
        try {
            // A file manager copies file:///home/me/Music, which Path.of would treat as relative.
            Path path = text.startsWith("file:")
                    ? Path.of(URI.create(text))
                    : Path.of(text).toAbsolutePath();
            return Files.exists(path) ? path : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static Track track(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return new Track(dot < 0 ? name : name.substring(0, dot), path.toUri().toString(), 0);
    }
}
