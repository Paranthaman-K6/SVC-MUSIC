package dev.local.vcmusic;

/** Little-endian 16 bit PCM helpers, kept out of the process code so they can be tested. */
public final class Pcm {

    private Pcm() {
    }

    public static short[] frame16le(byte[] bytes, int offset, int samples) {
        short[] frame = new short[samples];
        for (int i = 0; i < samples; i++) {
            int lo = bytes[offset + 2 * i] & 0xFF;
            int hi = bytes[offset + 2 * i + 1];
            frame[i] = (short) ((hi << 8) | lo);
        }
        return frame;
    }
}
