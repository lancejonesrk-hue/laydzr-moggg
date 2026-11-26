package gg.laydzr.plus;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.math.RoundingMode;

/** Blue graphite status card inspired by the user's supplied HUD reference. */
public final class LaydzrHud {
    private static boolean enabled = true;
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(LaydzrPlusClient.MOD_ID, "status_hud");

    private LaydzrHud() {}

    public static void register() {
        HudElementRegistry.addLast(HUD_ID, LaydzrHud::extract);
    }

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }

    private static void extract(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.getDebugOverlay().showDebugScreen()) return;

        int right = g.guiWidth() - 10;
        int x = right - 158;
        int y = 10;
        int h = 112;

        g.fill(x, y, right, y + h, 0xD90A101B);
        outline(g, x, y, 158, h, 0xFF3188D8);
        g.fill(x + 1, y + 1, right - 1, y + 3, 0xFF55B8FF);

        String playerName = mc.player.getName().getString();
        text(g, "LAYDZR", x + 9, y + 8, 0xFF8BCBFF);
        rightText(g, playerName, right - 9, y + 8, 0xFFEAF5FF);
        g.fill(x + 8, y + 23, right - 8, y + 24, 0xFF294A68);

        var net = TrackerState.INSTANCE.net();
        int netColor = net.signum() >= 0 ? 0xFF6DD8FF : 0xFFFF8292;
        text(g, "SESSION NET", x + 9, y + 31, 0xFF8FA6BC);
        rightText(g, money(net), right - 9, y + 29, netColor);

        text(g, "IN", x + 9, y + 51, 0xFF8FA6BC);
        text(g, money(TrackerState.INSTANCE.stats().incoming()), x + 32, y + 51, 0xFF6DD8FF);
        text(g, "OUT", x + 9, y + 67, 0xFF8FA6BC);
        text(g, money(TrackerState.INSTANCE.stats().outgoing()), x + 32, y + 67, 0xFFFF8292);
        text(g, "PAYMENTS", x + 9, y + 88, 0xFF8FA6BC);
        rightText(g, Integer.toString(TrackerState.INSTANCE.stats().entries().size()),
                right - 9, y + 88, 0xFFB9DFFF);

        int ribbonY = y + h + 6;
        g.fill(x, ribbonY, right, ribbonY + 27, 0xE60A1421);
        outline(g, x, ribbonY, 158, 27, 0xFF3188D8);
        text(g, "TOTAL FLIP P/L", x + 8, ribbonY + 9, 0xFF8BCBFF);
        var realized = PortfolioLedger.totalProfitLoss();
        int realizedColor = realized.signum() >= 0 ? 0xFF6DD8FF : 0xFFFF8292;
        rightText(g, money(realized), right - 8, ribbonY + 8, realizedColor);

        MusicNowPlaying track = SpotifyBridge.nowPlaying();
        int musicY = ribbonY + 33;
        g.fill(x, musicY, right, musicY + 43, 0xE60A1421);
        outline(g, x, musicY, 158, 43, 0xFF3188D8);
        text(g, "NOW PLAYING", x + 8, musicY + 5, 0xFF8BCBFF);
        String song = track.hasTrack() ? track.trackName() : SpotifyBridge.status();
        String artist = track.hasTrack() ? track.artistName() : "Press O to connect";
        text(g, trim(song, 22), x + 8, musicY + 17, 0xFFEAF5FF);
        text(g, trim(artist, 23), x + 8, musicY + 29, 0xFF8FA6BC);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, Component.literal(value), x, y, color);
    }

    private static void rightText(GuiGraphicsExtractor g, String value, int right, int y, int color) {
        text(g, value, right - Minecraft.getInstance().font.width(value), y, color);
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String money(java.math.BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
