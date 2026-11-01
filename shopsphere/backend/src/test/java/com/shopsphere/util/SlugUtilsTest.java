package com.shopsphere.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilsTest {

    @Test
    @DisplayName("Converts standard title to slug")
    void testStandardTitle() {
        String input = "Men's Running Shoes - Pro Edition!";
        String slug = SlugUtils.toSlug(input);
        assertThat(slug).isEqualTo("mens-running-shoes-pro-edition");
    }

    @Test
    @DisplayName("Handles accents and diacritics")
    void testAccentsAndDiacritics() {
        String input = "Café & Crème Brûlée Elegance";
        String slug = SlugUtils.toSlug(input);
        assertThat(slug).isEqualTo("cafe-creme-brulee-elegance");
    }

    @Test
    @DisplayName("Handles multiple whitespaces and consecutive hyphens")
    void testWhitespaceAndHyphens() {
        String input = "  Ultra   High---Definition    4K   ";
        String slug = SlugUtils.toSlug(input);
        assertThat(slug).isEqualTo("ultra-high-definition-4k");
    }

    @Test
    @DisplayName("Handles empty and null inputs safely")
    void testEmptyAndNull() {
        assertThat(SlugUtils.toSlug(null)).isEmpty();
        assertThat(SlugUtils.toSlug("")).isEmpty();
        assertThat(SlugUtils.toSlug("    ")).isEmpty();
    }
}
