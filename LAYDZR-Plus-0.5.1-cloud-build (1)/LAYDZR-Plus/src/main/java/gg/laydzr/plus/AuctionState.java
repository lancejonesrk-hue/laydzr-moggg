package gg.laydzr.plus;

import java.math.BigDecimal;
import java.time.Instant;

/** Local auction snapshot and completed flip calculations. */
public final class AuctionState {
    public record Snapshot(String item, int quantity, BigDecimal buyTotal,
                           BigDecimal estimatedWorth, String topBidder,
                           BigDecimal topBid, int bids, boolean active,
                           String updatedAt) {}

    private static String item = "No auction tracked";
    private static int quantity;
    private static BigDecimal buyTotal = BigDecimal.ZERO;
    private static BigDecimal estimatedWorth = BigDecimal.ZERO;
    private static String topBidder = "—";
    private static BigDecimal topBid = BigDecimal.ZERO;
    private static int bids;
    private static boolean active;
    private static BigDecimal lastProfitLoss = BigDecimal.ZERO;
    private static String lastItem = "No completed flip yet";

    private AuctionState() {}

    public static synchronized void start(String itemName, int count, BigDecimal cost) {
        if (itemName == null || itemName.isBlank()) throw new IllegalArgumentException("Item name cannot be empty");
        if (count < 1) throw new IllegalArgumentException("Quantity must be at least 1");
        if (cost == null || cost.signum() < 0) throw new IllegalArgumentException("Buy total cannot be negative");
        item = itemName.trim();
        quantity = count;
        buyTotal = cost;
        estimatedWorth = BigDecimal.ZERO;
        topBidder = "—";
        topBid = BigDecimal.ZERO;
        bids = 0;
        active = true;
    }

    public static synchronized void bid(String bidder, BigDecimal amount) {
        if (!active) throw new IllegalStateException("Start an auction first");
        if (bidder == null || bidder.isBlank()) throw new IllegalArgumentException("Bidder name cannot be empty");
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("Bid cannot be negative");
        if (amount.compareTo(topBid) <= 0) throw new IllegalArgumentException("Bid must be higher than the current top bid");
        topBidder = bidder.trim();
        topBid = amount;
        bids++;
    }

    public static synchronized void worth(BigDecimal amount) {
        if (!active) throw new IllegalStateException("Start an auction first");
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("Estimated worth cannot be negative");
        estimatedWorth = amount;
    }

    public static synchronized BigDecimal finish(BigDecimal sellTotal) throws java.io.IOException {
        if (!active) throw new IllegalStateException("No active auction to finish");
        if (sellTotal == null || sellTotal.signum() < 0) throw new IllegalArgumentException("Sale total cannot be negative");
        PortfolioLedger.add(new PortfolioLedger.Trade(Instant.now().toString(), item, quantity,
                buyTotal, sellTotal));
        lastProfitLoss = sellTotal.subtract(buyTotal);
        lastItem = item;
        active = false;
        return lastProfitLoss;
    }

    public static synchronized void clear() {
        item = "No auction tracked";
        quantity = 0;
        buyTotal = BigDecimal.ZERO;
        estimatedWorth = BigDecimal.ZERO;
        topBidder = "—";
        topBid = BigDecimal.ZERO;
        bids = 0;
        active = false;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(item, quantity, buyTotal, estimatedWorth, topBidder,
                topBid, bids, active, Instant.now().toString());
    }
    public static synchronized BigDecimal lastProfitLoss() { return lastProfitLoss; }
    public static synchronized String lastItem() { return lastItem; }
}
