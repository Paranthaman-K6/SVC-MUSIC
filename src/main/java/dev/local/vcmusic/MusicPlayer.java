package dev.local.vcmusic;

import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel;
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns the queue and the audio stream. The merge hook runs on Simple Voice Chat's mic thread,
 * so everything shared with the UI is synchronized or volatile.
 */
public final class MusicPlayer {

    public static final MusicPlayer INSTANCE = new MusicPlayer();

    private static final Logger LOG = LoggerFactory.getLogger("vcmusic");
    /** How long to wait for a microphone frame before telling the user voice chat is off. */
    private static final long MIC_SILENCE_NANOS = 1_500_000_000L;

    private final List<Track> queue = new ArrayList<>();
    private final AudioGain gain = new AudioGain();
    private Track current;
    private AudioStream stream;
    private volatile boolean paused;
    private volatile boolean buffering;
    private volatile ClientStaticAudioChannel localChannel;
    private volatile long startedAt;
    private volatile long lastMicFrameAt;
    private volatile long framesPlayed;
    private String lastError = "";
    /** Identifies the active stream so a late end-callback cannot skip the track that replaced it. */
    private int generation;

    public synchronized void play(Track track) {
        closeStream();
        current = track;
        paused = false;
        buffering = true;
        lastError = "";
        startedAt = System.nanoTime();
        lastMicFrameAt = 0;
        framesPlayed = 0;
        gain.reset();
        int gen = ++generation;
        try {
            stream = new AudioStream(track.url(), MusicConfig.INSTANCE, () -> onStreamEnd(gen));
            stream.start();
            LOG.info("Playing '{}'", track.title());
        } catch (Exception e) {
            stream = null;
            current = null;
            buffering = false;
            lastError = "yt-dlp/ffmpeg failed to start - see config/vcmusic/config.json";
            LOG.warn("Could not start '{}': {}", track.title(), e.toString());
        }
    }

    public synchronized void enqueue(Track track) {
        if (current == null) {
            play(track);
        } else {
            queue.add(track);
        }
    }

    public synchronized void next() {
        if (queue.isEmpty()) {
            stop();
            return;
        }
        int index = MusicConfig.INSTANCE.shuffle ? ThreadLocalRandom.current().nextInt(queue.size()) : 0;
        play(queue.remove(index));
    }

    public synchronized void stop() {
        closeStream();
        generation++;
        current = null;
        queue.clear();
        paused = false;
        buffering = false;
        gain.reset();
    }

    public synchronized void togglePause() {
        paused = !paused;
    }

    public synchronized void setPaused(boolean value) {
        paused = value;
    }

    public synchronized boolean paused() {
        return paused;
    }

    public synchronized Track current() {
        return current;
    }

    public boolean buffering() {
        return buffering;
    }

    /** Milliseconds of audio actually sent, for the panel's progress bar. */
    public long positionMs() {
        return framesPlayed * 20L;
    }

    public synchronized String lastError() {
        return lastError;
    }

    /**
     * True when a track is playing but Simple Voice Chat has not handed us a microphone frame
     * since it started. That means voice chat is disabled or muted, so nothing is transmitted.
     */
    public boolean voiceChatSilent() {
        if (current() == null || paused) {
            return false;
        }
        long last = lastMicFrameAt;
        long since = last > startedAt ? last : startedAt;
        return System.nanoTime() - since > MIC_SILENCE_NANOS;
    }

    public synchronized List<Track> queueSnapshot() {
        return new ArrayList<>(queue);
    }

    private synchronized void onStreamEnd(int gen) {
        if (gen != generation) {
            return;
        }
        if (stream != null && stream.framesRead() == 0) {
            String reason = stream.error();
            lastError = reason.isEmpty() ? "Track failed to load" : reason;
            LOG.warn("Track failed: {} - {}", current == null ? "?" : current.title(), lastError);
        }
        Minecraft.getInstance().execute(this::next);
    }

    private void closeStream() {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    public void resetLocalChannel() {
        localChannel = null;
    }

    /**
     * Called once per microphone frame (~50 Hz). Merges one music frame into the outgoing voice
     * audio and plays the same frame locally, because Simple Voice Chat does not echo your own
     * audio back to you.
     */
    public void onMerge(MergeClientSoundEvent event) {
        lastMicFrameAt = System.nanoTime();
        if (paused) {
            return;
        }
        AudioStream s;
        synchronized (this) {
            s = stream;
        }
        if (s == null) {
            return;
        }
        short[] frame = s.poll();
        if (frame == null) {
            return;
        }
        buffering = false;
        framesPlayed++;
        MusicConfig config = MusicConfig.INSTANCE;
        VoicechatClientApi api = event.getVoicechat();
        boolean speaking = api.isTalking() || api.isPushToTalkKeyPressed();
        short[] mixed = gain.apply(frame, config.volume, config.duck && speaking, config.duckLevel);

        event.mergeAudio(mixed);

        ClientStaticAudioChannel channel = localChannel(api);
        if (channel != null) {
            channel.play(config.localVolume == 1F ? mixed : AudioGain.scale(mixed, config.localVolume));
        }
    }

    private ClientStaticAudioChannel localChannel(VoicechatClientApi api) {
        if (localChannel == null) {
            try {
                localChannel = api.createStaticAudioChannel(UUID.randomUUID());
            } catch (Exception e) {
                return null;
            }
        }
        return localChannel;
    }
}
