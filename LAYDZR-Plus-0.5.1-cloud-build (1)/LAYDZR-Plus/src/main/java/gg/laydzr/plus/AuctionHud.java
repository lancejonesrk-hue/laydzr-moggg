package gg.laydzr.plus;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Compact blue auction card modeled after the supplied visual reference. */
public final class AuctionHud {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(LaydzrPlusClient.MOD_ID, "auction_hud");
    private static boolean enabled = true;
    private AuctionHud() {}

    public static void register() { HudElementRegistry.addLast(HUD_ID, AuctionHud::render); }
    public static void setEnabled(boolean value) { enabled = value; }

    private static void render(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || mc.gui.getDebugOverlay().showDebugScreen()) return;
        AuctionState.Snapshot s = AuctionState.snapshot();
        int w = 174, h = 124, x = g.guiWidth() - w - 10, y = 174;
        g.fill(x, y, x + w, y + h, 0xE90A1018);
        outline(g, x, y, w, h, LaydzrTheme.BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, 0xFF55B8FF);
        text(g, "LAYDZR AUCTION", x + 8, y + 7, LaydzrTheme.BRAND);
        text(g, s.active() ? "LIVE / TRACKING" : "NO ACTIVE AUCTION", x + 8, y + 21, LaydzrTheme.MUTED);
        g.fill(x + 8, y + 34, x + w - 8, y + 35, LaydzrTheme.BORDER_SOFT);
        text(g, trim(s.item(), 22) + (s.quantity() > 0 ? " x" + s.quantity() : ""), x + 8, y + 41, LaydzrTheme.TEXT);
        text(g, "TOP BIDDER", x + 8, y + 57, LaydzrTheme.MUTED);
        text(g, trim(s.topBidder(), 14) + "  " + money(s.topBid()), x + 82, y + 57, LaydzrTheme.TEXT);
        text(g, "WORTH " + money(s.estimatedWorth()), x + 8, y + 72, LaydzrTheme.MUTED);
        text(g, "BIDS " + s.bids(), x + 8, y + 87, LaydzrTheme.MUTED);
        BigDecimal pl = s.active() ? s.estimatedWorth().subtract(s.buyTotal()) : AuctionState.lastProfitLoss();
        int color = pl.signum() >= 0 ? LaydzrTheme.GREEN : LaydzrTheme.RED;
        String label = s.active() ? "EST. P/L " : "LAST P/L ";
        text(g, label + money(pl), x + 72, y + 87, color);
        text(g, "TOTAL FLIP P/L " + money(PortfolioLedger.totalProfitLoss()), x + 8, y + 100, LaydzrTheme.BRAND);
        text(g, "MADE BY LAYDZR", x + 8, y + 112, LaydzrTheme.MUTED);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c); g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c); g.fill(x + w - 1, y, x + w, y + h, c);
    }
    private static void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, Component.literal(value), x, y, color);
    }
    private static String trim(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }
    private static String money(BigDecimal v) { return "$" + v.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
}
