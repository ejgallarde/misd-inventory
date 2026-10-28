package ph.gov.phlpost.inventory.misddashboard.util;

import java.math.BigDecimal;

/** Money values in audit notes. */
public final class Amounts {

    private Amounts() {
    }

    /** Equal by value, so 1500 and 1500.00 (different BigDecimal scales) match. */
    public static boolean same(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    public static String display(BigDecimal amount) {
        return amount == null ? "None" : amount.toPlainString();
    }

    /** "; <label>: old -> new" when the amount changed, otherwise "". */
    public static String changeNote(String label, BigDecimal before, BigDecimal after) {
        return same(before, after) ? "" : "; " + label + ": " + display(before) + " -> " + display(after);
    }
}
