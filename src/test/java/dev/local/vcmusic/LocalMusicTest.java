package dev.local.vcmusic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMusicTest {

    @Test
    void readsAudioFilesFromAFolderAndSkipsOthers(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("album"));
        Files.writeString(dir.resolve("one.mp3"), "");
        Files.writeString(dir.resolve("album/two.FLAC"), "");
        Files.writeString(dir.resolve("notes.txt"), "");

        List<Track> tracks = LocalMusic.scan(dir.toString());

        Set<String> titles = tracks.stream().map(Track::title).collect(Collectors.toSet());
        assertEquals(Set.of("one", "two"), titles);
        assertTrue(tracks.stream().allMatch(t -> t.url().startsWith("file:")));
    }

    @Test
    void plainTextIsNotAPath() throws Exception {
        assertFalse(LocalMusic.isPath("look at me twice"));
        assertNull(LocalMusic.scan("look at me twice"));
    }

    @Test
    void singleFileIsOneSong(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("song.ogg");
        Files.writeString(file, "");

        List<Track> tracks = LocalMusic.scan(file.toString());

        assertEquals(1, tracks.size());
        assertEquals("song", tracks.get(0).title());
        // A file manager copies file:/// URIs, which must resolve to the same file.
        assertEquals("song", LocalMusic.scan(file.toUri().toString()).get(0).title());
    }
}
