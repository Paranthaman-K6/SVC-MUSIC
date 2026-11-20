package dev.local.vcmusic;

import java.util.ArrayList;
import java.util.List;

/**
 * Smoothed gain applied to every music frame. Attack/release are per 20 ms frame.
 */
public final class AudioGain {

    public static final int FRAME_SIZE = 960;

    private static final float ATTACK = 0.25f;
    private static final float RELEASE = 0.08f;

    private float gain = 1f;

    /**
     * @param in       one 960 sample frame
     * @param volume   user volume (0..1)
     * @param ducking  whether the player is speaking right now
     * @param duckLevel music level while ducking (0..1)
     */
    public short[] apply(short[] in, float volume, boolean ducking, float duckLevel) {
        float target = ducking ? duckLevel : 1f;
        gain += (target - gain) * (target < gain ? ATTACK : RELEASE);
        float g = gain * volume;
        short[] out = new short[in.length];
        for (int i = 0; i < in.length; i++) {
            int v = Math.round(in[i] * g);
            out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
        }
        return out;
    }

    public float gain() {
        return gain;
    }

    public void reset() {
        gain = 1f;
    }

    /** Utility for tests and the UI. */
    public static short[] scale(short[] in, float g) {
        short[] out = new short[in.length];
        for (int i = 0; i < in.length; i++) {
            int v = Math.round(in[i] * g);
            out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
        }
        return out;
    }

    public static List<short[]> frames(short[] pcm) {
        List<short[]> out = new ArrayList<>();
        for (int off = 0; off + FRAME_SIZE <= pcm.length; off += FRAME_SIZE) {
            short[] f = new short[FRAME_SIZE];
            System.arraycopy(pcm, off, f, 0, FRAME_SIZE);
            out.add(f);
        }
        return out;
    }
}
