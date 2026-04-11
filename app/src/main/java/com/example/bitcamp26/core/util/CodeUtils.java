package com.example.bitcamp26.core.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Utility methods for generating and validating short room/lobby codes.
 */
public final class CodeUtils {

    private static final String ALLOWED_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeUtils() {
        // Utility class
    }

    /**
     * Generates a random 6-character lobby code.
     */
    public static String generateCode() {
        return generateCode(DEFAULT_CODE_LENGTH);
    }

    /**
     * Generates a random code with the requested length.
     * Falls back to the default length if the provided value is invalid.
     */
    public static String generateCode(int length) {
        if (length <= 0) {
            length = DEFAULT_CODE_LENGTH;
        }

        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALLOWED_CHARS.length());
            builder.append(ALLOWED_CHARS.charAt(index));
        }
        return builder.toString();
    }

    /**
     * Normalizes user input by trimming spaces, removing internal spaces,
     * and converting to uppercase.
     */
    public static String normalizeCode(String rawCode) {
        if (rawCode == null) {
            return "";
        }

        return rawCode
                .trim()
                .replace(" ", "")
                .toUpperCase(Locale.US);
    }

    /**
     * Returns true if the code is valid using the default code length.
     */
    public static boolean isValidCode(String code) {
        return isValidCode(code, DEFAULT_CODE_LENGTH);
    }

    /**
     * Returns true if the code is non-empty, has the expected length,
     * and contains only allowed characters.
     */
    public static boolean isValidCode(String code, int expectedLength) {
        String normalized = normalizeCode(code);

        if (expectedLength <= 0) {
            expectedLength = DEFAULT_CODE_LENGTH;
        }

        if (normalized.length() != expectedLength) {
            return false;
        }

        for (int i = 0; i < normalized.length(); i++) {
            if (ALLOWED_CHARS.indexOf(normalized.charAt(i)) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a formatted display version of a code such as "ABC 123"
     * for easier reading. If the code is too short, the normalized code is returned.
     */
    public static String formatCodeForDisplay(String code) {
        String normalized = normalizeCode(code);

        if (normalized.length() <= 3) {
            return normalized;
        }

        return normalized.substring(0, 3) + " " + normalized.substring(3);
    }
}
