package ph.gov.phlpost.inventory.misddashboard.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public final class TextUtils {

    private static final Logger log = LoggerFactory.getLogger(TextUtils.class);

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

    public static double parseLenientDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse numeric value from '{}', defaulting to 0", value);
            return 0;
        }
    }
}
