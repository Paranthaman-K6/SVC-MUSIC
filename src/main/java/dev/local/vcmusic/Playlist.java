package dev.local.vcmusic;

import java.util.List;

/** A named list of songs the user builds themselves. */
public record Playlist(String name, List<Track> tracks) {

    /** False when the song is already in the playlist. */
    public boolean add(Track track) {
        if (tracks.stream().anyMatch(t -> t.url().equals(track.url()))) {
            return false;
        }
        tracks.add(track);
        return true;
    }

    public boolean remove(Track track) {
        return tracks.removeIf(t -> t.url().equals(track.url()));
    }
}
