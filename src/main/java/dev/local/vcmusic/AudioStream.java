package dev.local.vcmusic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * One track: yt-dlp | ffmpeg (or a local file) -> 48 kHz mono s16le -> 960 sample frames.
 * <p>
 * The queue is blocking on purpose. It gives backpressure to ffmpeg (and yt-dlp), which keeps
 * the stream at real time and makes pause work by simply not polling. 20 frames = 400 ms.
 */
public final class AudioStream {

    private static final int QUEUE_FRAMES = 20;

    private final ArrayBlockingQueue<short[]> frames = new ArrayBlockingQueue<>(QUEUE_FRAMES);
    private final Process ytdlp;
    private final Process ffmpeg;
    private final Thread pump;
    private final Thread reader;
    private final Thread ytdlpErr;
    private final Thread ffmpegErr;
    private volatile boolean closed;
    /** Last non-blank stderr line of either process; the reason a track failed to load. */
    private volatile String stderr = "";
    private int framesRead;

    public AudioStream(String url, MusicConfig config, Runnable onEnd) throws IOException {
        // Local files skip yt-dlp and go straight into ffmpeg.
        boolean local = url.startsWith("file:");
        String input = local ? Path.of(URI.create(url)).toString() : "pipe:0";
        Process ytdlp = local ? null : new ProcessBuilder(MusicConfig.resolve(config.ytdlp),
                "--no-playlist", "--no-warnings", "--no-progress",
                "-f", "bestaudio/best", "-o", "-", url)
                .start();
        Process ffmpeg;
        try {
            ffmpeg = new ProcessBuilder(MusicConfig.resolve(config.ffmpeg),
                    "-hide_banner", "-loglevel", "error",
                    "-re", "-i", input,
                    "-f", "s16le", "-ar", "48000", "-ac", "1", "pipe:1")
                    .start();
        } catch (IOException e) {
            if (ytdlp != null) {
                ytdlp.destroy();
            }
            throw e;
        }
        this.ytdlp = ytdlp;
        this.ffmpeg = ffmpeg;

        pump = ytdlp == null ? null
                : new Thread(() -> transfer(ytdlp.getInputStream(), ffmpeg.getOutputStream()), "vcmusic-pump");
        reader = new Thread(() -> read(ffmpeg.getInputStream(), onEnd), "vcmusic-reader");
        ytdlpErr = ytdlp == null ? null : stderrThread(ytdlp, "vcmusic-ytdlp-err");
        ffmpegErr = stderrThread(ffmpeg, "vcmusic-ffmpeg-err");
        if (pump != null) {
            pump.setDaemon(true);
        }
        reader.setDaemon(true);
    }

    public void start() {
        if (pump != null) {
            pump.start();
        }
        reader.start();
        if (ytdlpErr != null) {
            ytdlpErr.start();
        }
        ffmpegErr.start();
    }

    private Thread stderrThread(Process process, String name) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.isBlank()) {
                        stderr = line.trim();
                    }
                }
            } catch (IOException ignored) {
            }
        }, name);
        t.setDaemon(true);
        return t;
    }

    private void transfer(InputStream in, OutputStream out) {
        byte[] buf = new byte[8192];
        try (in; out) {
            int n;
            while (!closed && (n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
        } catch (IOException ignored) {
            // A closed process is the normal way this ends.
        }
    }

    private void read(InputStream in, Runnable onEnd) {
        byte[] buf = new byte[AudioGain.FRAME_SIZE * 2];
        try (in) {
            while (!closed) {
                int n = in.readNBytes(buf, 0, buf.length);
                if (n < buf.length) {
                    break;
                }
                short[] frame = Pcm.frame16le(buf, 0, AudioGain.FRAME_SIZE);
                frames.put(frame);
                framesRead++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        }
        if (!closed) {
            onEnd.run();
        }
    }

    /** Frames delivered so far. Zero after a natural end means yt-dlp or ffmpeg gave us nothing. */
    public int framesRead() {
        return framesRead;
    }

    /** Last error line printed by yt-dlp or ffmpeg, empty if neither complained. */
    public String error() {
        return stderr;
    }

    /** Non-blocking. Null means the buffer is empty (start of track or a stall). */
    public short[] poll() {
        return frames.poll();
    }

    public void close() {
        closed = true;
        if (ytdlp != null) {
            ytdlp.destroy();
        }
        ffmpeg.destroy();
        reader.interrupt();
        if (pump != null) {
            pump.interrupt();
        }
    }
}
