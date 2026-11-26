package gg.laydzr.plus;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Local transaction ledger. It records values entered by the player;
 * it does not claim to read private Donut SMP auction/server data.
 */
public final class PortfolioLedger {
    public record Trade(String timestamp, String itemId, int quantity,
                        BigDecimal buyTotal, BigDecimal sellTotal) {
        public BigDecimal profitLoss() {
            return sellTotal.subtract(buyTotal);
        }
    }

    private static final List<Trade> TRADES = new ArrayList<>();
    private static Path file;

    private PortfolioLedger() {}

    public static synchronized void load(Path path) throws IOException {
        file = path;
        TRADES.clear();
        if (!Files.exists(file)) {
            Files.writeString(file, "timestamp,item_id,quantity,buy_total,sell_total\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            return;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split(",", -1);
            if (p.length != 5) continue;
            try {
                TRADES.add(new Trade(p[0], p[1], Integer.parseInt(p[2]),
                        new BigDecimal(p[3]), new BigDecimal(p[4])));
            } catch (RuntimeException ignored) {
                // Skip malformed rows without losing the rest of the ledger.
            }
        }
    }

    public static synchronized void add(Trade trade) throws IOException {
        if (file == null) throw new IllegalStateException("Ledger not loaded");
        TRADES.add(trade);
        String row = String.join(",", csv(trade.timestamp()), csv(trade.itemId()),
                Integer.toString(trade.quantity()), trade.buyTotal().toPlainString(),
                trade.sellTotal().toPlainString()) + "\n";
        Files.writeString(file, row, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    public static synchronized List<Trade> trades() {
        return Collections.unmodifiableList(new ArrayList<>(TRADES));
    }

    public static synchronized BigDecimal totalProfitLoss() {
        return TRADES.stream().map(Trade::profitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static synchronized BigDecimal totalSpent() {
        return TRADES.stream().map(Trade::buyTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static synchronized BigDecimal totalSales() {
        return TRADES.stream().map(Trade::sellTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String csv(String value) {
        return value.replace(",", "_").replace("\n", " ");
    }
}
