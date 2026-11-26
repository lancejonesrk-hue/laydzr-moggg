package gg.laydzr.plus;

/** Normalized now-playing data for the future Spotify/local-player bridge. */
public record MusicNowPlaying(
        String trackName,
        String artistName,
        String albumName,
        String artworkUrl,
        long progressMillis,
        long durationMillis,
        PlaybackState playbackState
) {
    public enum PlaybackState { PLAYING, PAUSED, UNKNOWN }

    public MusicNowPlaying {
        trackName = safe(trackName);
        artistName = safe(artistName);
        albumName = safe(albumName);
        artworkUrl = safe(artworkUrl);
        progressMillis = Math.max(0, progressMillis);
        durationMillis = Math.max(0, durationMillis);
        playbackState = playbackState == null ? PlaybackState.UNKNOWN : playbackState;
    }

    public boolean hasTrack() {
        return !trackName.isBlank() && !artistName.isBlank();
    }

    public int progressPercent() {
        if (durationMillis <= 0) return 0;
        return (int) Math.min(100, progressMillis * 100 / durationMillis);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
