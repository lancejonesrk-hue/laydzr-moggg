package gg.laydzr.plus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Spotify Web API bridge using OAuth Authorization Code + PKCE (no client secret). */
public final class SpotifyBridge {
    public static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String AUTHORIZE = "https://accounts.spotify.com/authorize";
    private static final String TOKEN = "https://accounts.spotify.com/api/token";
    private static final String API = "https://api.spotify.com/v1";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LAYDZR-Spotify"); t.setDaemon(true); return t;
    });
    private static final AtomicReference<MusicNowPlaying> NOW = new AtomicReference<>(
            new MusicNowPlaying("", "", "", "", 0, 0, MusicNowPlaying.PlaybackState.UNKNOWN));
    private static volatile String clientId = "";
    private static volatile String accessToken = "";
    private static volatile String refreshToken = "";
    private static volatile long expiresAt;
    private static volatile String status = "Not connected";
    private static volatile Path tokenFile;
    private static volatile HttpServer callbackServer;

    private SpotifyBridge() {}

    public static synchronized void initialize(Path configFile) {
        tokenFile = configFile.resolveSibling("spotify-token.properties");
        try {
            if (Files.exists(tokenFile)) {
                Properties p = new Properties();
                try (var in = Files.newInputStream(tokenFile)) { p.load(in); }
                refreshToken = p.getProperty("refresh_token", "");
            }
        } catch (Exception e) { status = "Could not read saved Spotify session"; }
        WORKER.scheduleAtFixedRate(SpotifyBridge::pollSafely, 2, 5, TimeUnit.SECONDS);
    }

    public static void setClientId(String id) { clientId = id == null ? "" : id.trim(); }
    public static String status() { return status; }
    public static MusicNowPlaying nowPlaying() { return NOW.get(); }
    public static boolean connected() { return !refreshToken.isBlank() || !accessToken.isBlank(); }

    public static synchronized void connect() {
        if (clientId.isBlank()) { status = "Add your Spotify Client ID to music.properties"; return; }
        try {
            if (callbackServer != null) callbackServer.stop(0);
            String verifier = randomUrlToken(64);
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            String state = randomUrlToken(24);
            callbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 8888), 0);
            callbackServer.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getRawQuery();
                String code = queryValue(query, "code");
                String returnedState = queryValue(query, "state");
                String error = queryValue(query, "error");
                String message;
                if (error != null) message = "Spotify authorization was cancelled. You can close this tab.";
                else if (code == null || !state.equals(returnedState)) message = "Spotify authorization failed. Return to Minecraft and try again.";
                else {
                    try { exchangeToken(code, verifier); message = "Spotify connected to LAYDZR+. You can close this tab."; }
                    catch (Exception e) { status = "Spotify token exchange failed: " + e.getMessage(); message = "Connection failed. Return to Minecraft for details."; }
                }
                byte[] body = ("<!doctype html><meta charset='utf-8'><title>LAYDZR+</title>" +
                        "<body style='background:#0a1018;color:#8bcBff;font:18px sans-serif;padding:40px'>" + message + "</body>")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
            });
            callbackServer.setExecutor(Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "LAYDZR-OAuth"); t.setDaemon(true); return t; }));
            callbackServer.start();
            String scopes = "user-read-currently-playing user-read-playback-state user-modify-playback-state";
            String url = AUTHORIZE + "?response_type=code&client_id=" + enc(clientId) + "&redirect_uri=" + enc(REDIRECT_URI)
                    + "&scope=" + enc(scopes) + "&state=" + enc(state) + "&code_challenge_method=S256&code_challenge=" + enc(challenge);
            if (!Desktop.isDesktopSupported()) { status = "Open this URL manually: " + url; return; }
            Desktop.getDesktop().browse(URI.create(url));
            status = "Waiting for Spotify authorization in your browser…";
        } catch (Exception e) { status = "Could not start Spotify login: " + e.getMessage(); }
    }

    public static void next() { command("POST", "/me/player/next"); }
    public static void previous() { command("POST", "/me/player/previous"); }
    public static void togglePlayback() {
        MusicNowPlaying p = NOW.get();
        command("PUT", p.playbackState() == MusicNowPlaying.PlaybackState.PLAYING ? "/me/player/pause" : "/me/player/play");
    }

    private static void command(String method, String path) {
        WORKER.execute(() -> {
            try {
                ensureAccessToken();
                HttpRequest req = HttpRequest.newBuilder(URI.create(API + path)).header("Authorization", "Bearer " + accessToken)
                        .method(method, HttpRequest.BodyPublishers.noBody()).timeout(Duration.ofSeconds(10)).build();
                HttpResponse<Void> response = HTTP.send(req, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 403) status = "Spotify controls may require Premium";
                else if (response.statusCode() >= 300 && response.statusCode() != 204) status = "Spotify control unavailable (HTTP " + response.statusCode() + ")";
                else status = "Spotify connected";
            } catch (Exception e) { status = "Spotify control failed: " + e.getMessage(); }
        });
    }

    private static void pollSafely() { try { poll(); } catch (Exception e) { if (!refreshToken.isBlank()) status = "Spotify metadata unavailable: " + e.getMessage(); } }
    private static void poll() throws Exception {
        if (refreshToken.isBlank() && accessToken.isBlank()) return;
        ensureAccessToken();
        HttpRequest req = HttpRequest.newBuilder(URI.create(API + "/me/player/currently-playing"))
                .header("Authorization", "Bearer " + accessToken).timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 204) { status = "Spotify connected · nothing playing"; NOW.set(empty()); return; }
        if (response.statusCode() == 401) { accessToken = ""; ensureAccessToken(); return; }
        if (response.statusCode() == 403) { status = "Spotify metadata access unavailable for this account"; return; }
        if (response.statusCode() >= 300) throw new IOException("HTTP " + response.statusCode());
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!root.has("item") || root.get("item").isJsonNull()) { NOW.set(empty()); return; }
        JsonObject item = root.getAsJsonObject("item");
        String artist = item.getAsJsonArray("artists").size() == 0 ? "" : item.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
        String art = "";
        if (item.has("album") && item.getAsJsonObject("album").has("images") && item.getAsJsonObject("album").getAsJsonArray("images").size() > 0)
            art = item.getAsJsonObject("album").getAsJsonArray("images").get(0).getAsJsonObject().get("url").getAsString();
        long progress = root.has("progress_ms") && !root.get("progress_ms").isJsonNull() ? root.get("progress_ms").getAsLong() : 0;
        long duration = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0;
        NOW.set(new MusicNowPlaying(item.get("name").getAsString(), artist,
                item.getAsJsonObject("album").get("name").getAsString(), art, progress, duration,
                root.has("is_playing") && root.get("is_playing").getAsBoolean() ? MusicNowPlaying.PlaybackState.PLAYING : MusicNowPlaying.PlaybackState.PAUSED));
        status = "Spotify connected";
    }

    private static synchronized void ensureAccessToken() throws Exception {
        if (!accessToken.isBlank() && System.currentTimeMillis() < expiresAt - 30_000) return;
        if (refreshToken.isBlank()) return;
        String form = "grant_type=refresh_token&refresh_token=" + enc(refreshToken) + "&client_id=" + enc(clientId);
        HttpRequest req = HttpRequest.newBuilder(URI.create(TOKEN)).header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> response = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) throw new IOException("refresh token HTTP " + response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        accessToken = json.get("access_token").getAsString();
        expiresAt = System.currentTimeMillis() + json.get("expires_in").getAsLong() * 1000;
        if (json.has("refresh_token")) { refreshToken = json.get("refresh_token").getAsString(); saveRefreshToken(); }
    }

    private static synchronized void exchangeToken(String code, String verifier) throws Exception {
        String form = "grant_type=authorization_code&code=" + enc(code) + "&redirect_uri=" + enc(REDIRECT_URI)
                + "&client_id=" + enc(clientId) + "&code_verifier=" + enc(verifier);
        HttpRequest req = HttpRequest.newBuilder(URI.create(TOKEN)).header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> response = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) throw new IOException("HTTP " + response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        accessToken = json.get("access_token").getAsString();
        refreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : "";
        expiresAt = System.currentTimeMillis() + json.get("expires_in").getAsLong() * 1000;
        saveRefreshToken(); status = "Spotify connected";
    }

    private static void saveRefreshToken() throws IOException {
        if (tokenFile == null || refreshToken.isBlank()) return;
        Properties p = new Properties(); p.setProperty("refresh_token", refreshToken);
        try (var out = Files.newOutputStream(tokenFile)) { p.store(out, "Private LAYDZR+ Spotify refresh token. Do not share this file."); }
    }
    private static MusicNowPlaying empty() { return new MusicNowPlaying("", "", "", "", 0, 0, MusicNowPlaying.PlaybackState.UNKNOWN); }
    private static String randomUrlToken(int bytes) { byte[] b = new byte[bytes]; new SecureRandom().nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String queryValue(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) { String[] kv = part.split("=", 2); if (kv.length == 2 && kv[0].equals(key)) return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8); }
        return null;
    }
}
