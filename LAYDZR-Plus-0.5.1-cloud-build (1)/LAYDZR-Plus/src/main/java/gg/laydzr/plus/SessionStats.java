package gg.laydzr.plus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory session calculations used by the tracker UI. */
public final class SessionStats {
    public record Entry(String player, BigDecimal amount,
                        PaymentMessageParser.Direction direction) {}

    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, BigDecimal> incomingByPlayer = new LinkedHashMap<>();
    private BigDecimal incoming = BigDecimal.ZERO;
    private BigDecimal outgoing = BigDecimal.ZERO;
    private int currentLossStreak;
    private int longestLossStreak;

    public synchronized void record(PaymentMessageParser.ParsedPayment payment) {
        entries.add(new Entry(payment.player(), payment.amount(), payment.direction()));
        if (payment.direction() == PaymentMessageParser.Direction.INCOMING) {
            incoming = incoming.add(payment.amount());
            incomingByPlayer.merge(payment.player(), payment.amount(), BigDecimal::add);
            currentLossStreak = 0;
        } else {
            outgoing = outgoing.add(payment.amount());
            currentLossStreak++;
            longestLossStreak = Math.max(longestLossStreak, currentLossStreak);
        }
    }

    /** Incoming minus outgoing; this is cash flow, not guaranteed gambling profit. */
    public synchronized BigDecimal net() { return incoming.subtract(outgoing); }
    public synchronized BigDecimal incoming() { return incoming; }
    public synchronized BigDecimal outgoing() { return outgoing; }
    public synchronized int currentLossStreak() { return currentLossStreak; }
    public synchronized int longestLossStreak() { return longestLossStreak; }
    public synchronized List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }
    public synchronized Map<String, BigDecimal> incomingByPlayer() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(incomingByPlayer));
    }
    public synchronized BigDecimal rakebackOwed(String player, BigDecimal percent) {
        if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Percent must be between 0 and 100");
        return incomingByPlayer.getOrDefault(player, BigDecimal.ZERO)
                .multiply(percent).divide(new BigDecimal("100"));
    }
}
