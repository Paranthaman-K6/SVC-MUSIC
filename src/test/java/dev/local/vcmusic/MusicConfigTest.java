package dev.local.vcmusic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MusicConfigTest {

    @Test
    void keepsAbsolutePathsAndFindsCommandsOnPath() {
        assertEquals("/bin/sh", MusicConfig.resolve("/bin/sh"));

        String sh = MusicConfig.resolve("sh");
        assertTrue(sh.startsWith("/") && sh.endsWith("/sh"), "should resolve sh to an absolute path, got " + sh);
    }

    @Test
    void leavesAnUnknownCommandAlone() {
        assertEquals("no-such-binary-xyz", MusicConfig.resolve("no-such-binary-xyz"));
    }
}
