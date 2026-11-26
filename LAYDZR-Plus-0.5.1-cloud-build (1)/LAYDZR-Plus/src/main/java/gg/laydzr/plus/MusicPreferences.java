package gg.laydzr.plus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

/** User music preferences. The three LAYDZR+ headliners are always kept first. */
public final class MusicPreferences {
    private static final List<String> HEADLINERS = List.of(
            "Lil Uzi Vert", "Juice WRLD", "Chief Keef"
    );

    private static final Properties DATA = new Properties();
    private static Path file;

    private MusicPreferences() {}

    public static synchronized void load(Path configFile) throws IOException {
        file = configFile;
        DATA.clear();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                DATA.load(in);
            }
        }
        // Keep the defaults explicit in the config so users can see/edit the list.
        if (DATA.getProperty("featured.artists") == null) {
            DATA.setProperty("featured.artists", String.join("|", HEADLINERS));
            save();
        }
        if (DATA.getProperty("spotify.client_id") == null) {
            DATA.setProperty("spotify.client_id", "");
            save();
        }
    }

    public static synchronized List<String> featuredArtists() {
        LinkedHashSet<String> artists = new LinkedHashSet<>(HEADLINERS);
        String configured = DATA.getProperty("featured.artists", "");
        for (String name : configured.split("\\|")) {
            String cleaned = name.trim();
            if (!cleaned.isEmpty()) artists.add(cleaned);
        }
        return List.copyOf(artists);
    }

    public static synchronized List<String> otherArtists() {
        List<String> result = new ArrayList<>();
        String configured = DATA.getProperty("other.artists", "");
        for (String name : configured.split("\\|")) {
            String cleaned = name.trim();
            if (!cleaned.isEmpty() && !featuredArtists().contains(cleaned)) result.add(cleaned);
        }
        return List.copyOf(new LinkedHashSet<>(result));
    }

    /** Replaces the additional artist list without removing the three main artists. */
    public static synchronized void setOtherArtists(List<String> artists) throws IOException {
        LinkedHashSet<String> clean = new LinkedHashSet<>();
        for (String artist : artists) {
            if (artist == null) continue;
            String name = artist.trim();
            if (!name.isEmpty() && !HEADLINERS.contains(name)) clean.add(name);
        }
        DATA.setProperty("other.artists", String.join("|", clean));
        save();
    }

    public static synchronized String spotifyClientId() {
        return DATA.getProperty("spotify.client_id", "").trim();
    }

    public static List<String> headliners() {
        return HEADLINERS;
    }

    private static void save() throws IOException {
        if (file == null) return;
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (OutputStream out = Files.newOutputStream(file)) {
            DATA.store(out, "LAYDZR+ music preferences");
        }
    }
}
