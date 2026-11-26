package gg.laydzr.plus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TrackerState {
    public static final TrackerState INSTANCE = new TrackerState();
    private final SessionStats session = new SessionStats();
    private final List<String> recent = new ArrayList<>();
    private boolean tracking = true;
    private int lossAlertThreshold = 5;

    private TrackerState() {}

    public synchronized void accept(String raw) {
        if (!tracking) return;
        PaymentMessageParser.parse(raw).ifPresent(payment -> {
            session.record(payment);
            recent.add(0, (payment.direction() == PaymentMessageParser.Direction.INCOMING ? "+" : "-")
                    + "$" + payment.amount().toPlainString() + "  " + payment.player());
            while (recent.size() > 12) recent.remove(recent.size() - 1);
        });
    }

    public synchronized SessionStats stats() { return session; }
    public synchronized List<String> recent() {
        return Collections.unmodifiableList(new ArrayList<>(recent));
    }
    public synchronized boolean tracking() { return tracking; }
    public synchronized void setTracking(boolean enabled) { tracking = enabled; }
    public synchronized int lossAlertThreshold() { return lossAlertThreshold; }
    public synchronized void setLossAlertThreshold(int threshold) {
        lossAlertThreshold = Math.max(2, Math.min(20, threshold));
    }
    public synchronized BigDecimal net() { return session.net(); }
}
