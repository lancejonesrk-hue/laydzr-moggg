package gg.laydzr.plus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the two documented DonutSMP system-chat payment formats.
 * Only call this for normal server chat messages, never private whispers.
 */
public final class PaymentMessageParser {
    public enum Direction { INCOMING, OUTGOING }

    public record ParsedPayment(String player, BigDecimal amount, Direction direction) {}

    private static final Pattern INCOMING = Pattern.compile(
            "^\\s*(.+?) paid you \\$([0-9][0-9,]*(?:\\.[0-9]+)?[kKmMbB]?)\\s*$");
    private static final Pattern OUTGOING = Pattern.compile(
            "^\\s*You paid (.+?) \\$([0-9][0-9,]*(?:\\.[0-9]+)?[kKmMbB]?)\\s*$");

    private PaymentMessageParser() {}

    public static Optional<ParsedPayment> parse(String plainText) {
        if (plainText == null) return Optional.empty();
        Matcher in = INCOMING.matcher(plainText);
        if (in.matches()) return build(in, Direction.INCOMING);
        Matcher out = OUTGOING.matcher(plainText);
        if (out.matches()) return build(out, Direction.OUTGOING);
        return Optional.empty();
    }

    private static Optional<ParsedPayment> build(Matcher matcher, Direction direction) {
        String player = matcher.group(1).trim();
        if (player.isEmpty()) return Optional.empty();
        try {
            return Optional.of(new ParsedPayment(player, parseAmount(matcher.group(2)), direction));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    static BigDecimal parseAmount(String raw) {
        String value = raw.replace(",", "").trim();
        BigDecimal multiplier = BigDecimal.ONE;
        char last = Character.toLowerCase(value.charAt(value.length() - 1));
        if (last == 'k' || last == 'm' || last == 'b') {
            value = value.substring(0, value.length() - 1);
            multiplier = switch (last) {
                case 'k' -> BigDecimal.valueOf(1_000L);
                case 'm' -> BigDecimal.valueOf(1_000_000L);
                case 'b' -> BigDecimal.valueOf(1_000_000_000L);
                default -> BigDecimal.ONE;
            };
        }
        return new BigDecimal(value).multiply(multiplier);
    }
}
