package gg.laydzr.plus;

/** Shared graphite + electric-blue visual palette for every LAYDZR+ surface. */
public final class LaydzrTheme {
    public static final int BACKDROP = 0xE90A1018;
    public static final int PANEL = 0xF0101722;
    public static final int PANEL_ALT = 0xFF172332;
    public static final int PANEL_HOVER = 0xFF203449;
    public static final int BORDER = 0xFF3188D8;
    public static final int BORDER_SOFT = 0xFF294A68;
    public static final int TEXT = 0xFFEAF5FF;
    public static final int MUTED = 0xFF8FA6BC;
    public static final int GREEN = 0xFF6DD8FF;
    public static final int RED = 0xFFFF8292;
    public static final int BRAND = 0xFF8BCBFF;

    private LaydzrTheme() {}

    public static void outline(net.minecraft.client.gui.GuiGraphics g,
                               int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y, x + 1, y + height, color);
        g.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static void panel(net.minecraft.client.gui.GuiGraphics g,
                             int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, PANEL);
        outline(g, x, y, width, height, BORDER_SOFT);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, BORDER);
    }
}
