package dev.local.vcmusic;

public record Track(String title, String url, int durationSeconds, String thumbnail) {

    public Track(String title, String url, int durationSeconds) {
        this(title, url, durationSeconds, null);
    }

    public String durationLabel() {
        if (durationSeconds <= 0) {
            return "--:--";
        }
        return String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
    }

    public String display() {
        return title + "  (" + durationLabel() + ")";
    }

    /** A row in the playlist list that names a playlist instead of a song. */
    public boolean isPlaylist() {
        return url.startsWith("playlist:");
    }

    public String playlistName() {
        return url.substring("playlist:".length());
    }
}
