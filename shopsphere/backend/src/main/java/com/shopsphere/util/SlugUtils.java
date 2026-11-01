package com.shopsphere.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern DUPLICATE_HYPHENS = Pattern.compile("-+");

    private SlugUtils() {
        // Utility class
    }

    /**
     * Converts arbitrary string into an SEO-friendly URL slug.
     * Example: "Men's Running Shoes - Pro Edition!" -> "mens-running-shoes-pro-edition"
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = DUPLICATE_HYPHENS.matcher(slug).replaceAll("-");

        // Trim leading and trailing hyphens
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }

        return slug.toLowerCase(Locale.ENGLISH);
    }
}
