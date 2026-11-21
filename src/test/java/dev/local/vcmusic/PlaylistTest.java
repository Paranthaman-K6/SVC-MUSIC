package dev.local.vcmusic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistTest {

    private static Track song(String url) {
        return new Track("song", url, 0);
    }

    @Test
    void addingTheSameSongTwiceKeepsOneCopy() {
        Playlist playlist = new Playlist("rock", new ArrayList<>());

        assertTrue(playlist.add(song("https://youtu.be/a")));
        assertFalse(playlist.add(song("https://youtu.be/a")));
        assertEquals(1, playlist.tracks().size());
    }

    @Test
    void removeDropsOnlyTheMatchingSong() {
        Playlist playlist = new Playlist("rock", new ArrayList<>(List.of(
                song("https://youtu.be/a"), song("https://youtu.be/b"))));

        assertTrue(playlist.remove(song("https://youtu.be/a")));
        assertFalse(playlist.remove(song("https://youtu.be/a")));
        assertEquals(1, playlist.tracks().size());
        assertEquals("https://youtu.be/b", playlist.tracks().get(0).url());
    }
}
