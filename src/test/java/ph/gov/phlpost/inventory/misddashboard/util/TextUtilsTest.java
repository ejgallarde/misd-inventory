package ph.gov.phlpost.inventory.misddashboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextUtilsTest {

    @Test
    void splitCsvTrimsAndDropsEmptyTokens() {
        assertThat(TextUtils.splitCsv(" Title , ,Tax Declaration,")).containsExactly("Title", "Tax Declaration");
        assertThat(TextUtils.splitCsv(null)).isEmpty();
        assertThat(TextUtils.splitCsv("   ")).isEmpty();
    }

    @Test
    void blankHandlingTreatsWhitespaceAsBlank() {
        assertThat(TextUtils.isBlank(null)).isTrue();
        assertThat(TextUtils.isBlank(" \t")).isTrue();
        assertThat(TextUtils.isBlank("x")).isFalse();
        assertThat(TextUtils.normalizeBlank(null)).isEmpty();
        assertThat(TextUtils.normalizeBlank("  Lot ")).isEqualTo("Lot");
    }
}
