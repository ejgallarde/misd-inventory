package ph.gov.phlpost.inventory.misddashboard.util;

import java.util.Arrays;
import java.util.List;

public final class TextUtils {

    private TextUtils() {
    }

    public static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String normalizeBlank(String value) {
        return value == null ? "" : value.trim();
    }
}
