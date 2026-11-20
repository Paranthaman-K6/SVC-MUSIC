package dev.local.vcmusic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioTest {

    @Test
    void decodesLittleEndianSignedSamples() {
        byte[] bytes = {0x01, 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0x80};
        short[] frame = Pcm.frame16le(bytes, 0, 3);
        assertEquals(1, frame[0]);
        assertEquals(-1, frame[1]);
        assertEquals(Short.MIN_VALUE, frame[2]);
    }

    @Test
    void ducksToLevelWhileSpeakingAndReleases() {
        AudioGain gain = new AudioGain();
        short[] frame = new short[AudioGain.FRAME_SIZE];

        for (int i = 0; i < 60; i++) {
            gain.apply(frame, 1F, true, 0.35F);
        }
        assertEquals(0.35F, gain.gain(), 0.01F, "should reach the duck level");

        for (int i = 0; i < 120; i++) {
            gain.apply(frame, 1F, false, 0.35F);
        }
        assertEquals(1F, gain.gain(), 0.01F, "should release back to full volume");
    }

    @Test
    void neverLeavesShortRange() {
        AudioGain gain = new AudioGain();
        short[] loud = new short[AudioGain.FRAME_SIZE];
        java.util.Arrays.fill(loud, Short.MAX_VALUE);

        short[] out = gain.apply(loud, 4F, false, 0.35F);
        for (short s : out) {
            assertEquals(Short.MAX_VALUE, s);
        }
    }

    @Test
    void splitsPcmIntoVoiceChatFrames() {
        short[] pcm = new short[AudioGain.FRAME_SIZE * 2];
        pcm[AudioGain.FRAME_SIZE] = 7;
        List<short[]> frames = AudioGain.frames(pcm);
        assertEquals(2, frames.size());
        assertEquals(7, frames.get(1)[0]);
        assertTrue(frames.stream().allMatch(f -> f.length == AudioGain.FRAME_SIZE));
    }
}
