package dev.local.vcmusic;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Saves one song as an mp3 into vcmusic-downloads next to the game. */
public final class Downloader {

    private static final int TIMEOUT_MINUTES = 10;

    private Downloader() {
    }

    public static Path directory() {
        return FabricLoader.getInstance().getGameDir().resolve("vcmusic-downloads");
    }

    /** Blocking; call it off the render thread. Returns the folder the song went to. */
    public static Path download(Track track, MusicConfig config) throws IOException {
        Path dir = directory();
        Files.createDirectories(dir);
        Process process = new ProcessBuilder(MusicConfig.resolve(config.ytdlp),
                "--no-playlist", "--no-warnings", "--no-progress",
                "-x", "--audio-format", "mp3", "--audio-quality", "0",
                // The game's PATH can be short, so point yt-dlp at the configured ffmpeg.
                "--ffmpeg-location", MusicConfig.resolve(config.ffmpeg),
                "-o", dir.resolve("%(title)s.%(ext)s").toString(),
                track.url())
                .redirectErrorStream(true)
                .start();
        String output;
        try (var in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try {
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new IOException("download timed out");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("download interrupted", e);
        }
        if (process.exitValue() != 0) {
            throw new IOException(lastLine(output));
        }
        return dir;
    }

    private static String lastLine(String output) {
        String[] lines = output.strip().split("\n");
        return lines.length == 0 || lines[lines.length - 1].isBlank()
                ? "yt-dlp failed" : lines[lines.length - 1].trim();
    }
}
