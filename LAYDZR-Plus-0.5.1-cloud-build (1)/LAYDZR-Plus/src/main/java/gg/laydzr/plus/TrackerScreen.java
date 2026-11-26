package gg.laydzr.plus;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.math.RoundingMode;
import java.util.List;

public final class TrackerScreen extends Screen {
    public TrackerScreen() {
        super(Component.literal("LAYDZR+"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int width = Math.min(520, this.width - 28);
        int height = Math.min(370, this.height - 28);
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;

        // Main frame and title strip.
        LaydzrTheme.panel(g, x, y, width, height);
        g.fill(x + 1, y + 1, x + width - 1, y + 34, LaydzrTheme.PANEL_ALT);
        g.fill(x + 12, y + 11, x + 15, y + 24, LaydzrTheme.BRAND);
        g.drawString(this.font, "LAYDZR+", x + 23, y + 9, LaydzrTheme.TEXT, false);
        g.drawString(this.font, "PAYMENT TRACKER", x + 92, y + 11, LaydzrTheme.MUTED, false);
        String status = TrackerState.INSTANCE.tracking() ? "TRACKING ON" : "PAUSED";
        int statusColor = TrackerState.INSTANCE.tracking() ? LaydzrTheme.GREEN : LaydzrTheme.MUTED;
        int statusWidth = this.font.width(status);
        g.drawString(this.font, status, x + width - statusWidth - 14, y + 11, statusColor, false);
        g.fill(x + 12, y + 34, x + width - 12, y + 35, LaydzrTheme.BORDER_SOFT);

        SessionStats stats = TrackerState.INSTANCE.stats();
        int gap = 9;
        int cardY = y + 47;
        int cardW = (width - 24 - gap * 2) / 3;
        metricCard(g, x + 12, cardY, cardW, 66, "MONEY IN", money(stats.incoming()), LaydzrTheme.GREEN);
        metricCard(g, x + 12 + cardW + gap, cardY, cardW, 66, "MONEY OUT", money(stats.outgoing()), LaydzrTheme.RED);
        metricCard(g, x + 12 + (cardW + gap) * 2, cardY, cardW, 66, "SESSION NET", money(stats.net()),
                stats.net().signum() >= 0 ? LaydzrTheme.GREEN : LaydzrTheme.RED);

        // Streak strip.
        int stripY = cardY + 77;
        LaydzrTheme.panel(g, x + 12, stripY, width - 24, 48);
        g.drawString(this.font, "STREAKS", x + 23, stripY + 8, LaydzrTheme.MUTED, false);
        g.drawString(this.font, "Current  " + stats.currentLossStreak(), x + 23, stripY + 25,
                LaydzrTheme.TEXT, false);
        g.drawString(this.font, "Worst  " + stats.longestLossStreak(), x + 150, stripY + 25,
                LaydzrTheme.TEXT, false);
        g.drawString(this.font, "Alert threshold  " + TrackerState.INSTANCE.lossAlertThreshold(),
                x + 280, stripY + 25, LaydzrTheme.MUTED, false);

        // Music panel: metadata is populated by the Spotify bridge when authorized.
        int musicY = stripY + 56;
        LaydzrTheme.panel(g, x + 12, musicY, width - 24, 42);
        MusicNowPlaying playing = SpotifyBridge.nowPlaying();
        String musicTitle = playing.hasTrack() ? playing.trackName() + "  —  " + playing.artistName() : SpotifyBridge.status();
        g.drawString(this.font, "MUSIC  ·  " + musicTitle, x + 23, musicY + 8, LaydzrTheme.TEXT, false);
        String featured = "FEATURED  " + String.join("  /  ", MusicPreferences.headliners());
        g.drawString(this.font, featured, x + 23, musicY + 24, LaydzrTheme.BRAND, false);

        // Recent payment activity.
        int listY = musicY + 51;
        int listH = Math.max(38, y + height - listY - 31);
        LaydzrTheme.panel(g, x + 12, listY, width - 24, listH);
        g.drawString(this.font, "RECENT PAYMENTS", x + 23, listY + 9, LaydzrTheme.MUTED, false);
        g.fill(x + 23, listY + 25, x + width - 23, listY + 26, LaydzrTheme.BORDER_SOFT);

        List<String> recent = TrackerState.INSTANCE.recent();
        int lineY = listY + 36;
        for (String line : recent) {
            if (lineY > listY + listH - 13) break;
            int color = line.startsWith("+") ? LaydzrTheme.GREEN : LaydzrTheme.RED;
            g.drawString(this.font, line, x + 23, lineY, color, false);
            lineY += 13;
        }
        if (recent.isEmpty()) {
            String empty = "Waiting for recognized DonutSMP payment messages";
            g.drawString(this.font, empty, x + 23, lineY + 3, LaydzrTheme.MUTED, false);
        }

        String footer = "L CLOSE  ·  O SPOTIFY LOGIN  ·  LAYDZR / DONUT SMP";
        int footerWidth = this.font.width(footer);
        g.drawString(this.font, footer, x + width - footerWidth - 13,
                y + height - 17, LaydzrTheme.MUTED, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void metricCard(GuiGraphics g, int x, int y, int w, int h,
                            String label, String value, int valueColor) {
        LaydzrTheme.panel(g, x, y, w, h);
        g.drawString(this.font, label, x + 9, y + 10, LaydzrTheme.MUTED, false);
        g.drawString(this.font, value, x + 9, y + 32, valueColor, false);
    }

    private static String money(java.math.BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
