package com.example.bitcamp26.navigation;

/**
 * Central route names used by AppNavigator and the rest of the app.
 *
 * These are lightweight string constants that identify screens and back-stack entries.
 * Keeping them in one place avoids typos and makes navigation code easier to maintain.
 *
 * This class is intentionally a utility-style constants holder:
 * - final class so it cannot be subclassed
 * - private constructor so it cannot be instantiated
 */
public final class Routes {

    /**
     * Route name for the authentication flow.
     *
     * This is mainly used as a semantic identifier because auth currently launches
     * through an Activity rather than a Fragment transaction.
     */
    public static final String AUTH = "auth";

    /**
     * Route name for the lobby screen.
     */
    public static final String LOBBY = "lobby";

    /**
     * Route name for the ready-check screen.
     */
    public static final String READY_CHECK = "ready_check";

    /**
     * Route name for the active match screen.
     */
    public static final String MATCH = "match";

    /**
     * Route name for the match results screen.
     */
    public static final String RESULTS = "results";

    /**
     * Private constructor to prevent instantiation.
     */
    private Routes() {
        // Utility class
    }
}
