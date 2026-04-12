package com.example.bitcamp26.core.util;

/**
 * Shared Firebase backend configuration used by both Functions and RTDB clients.
 *
 * Keeping these values explicit avoids subtle mismatches when the generated
 * Android config is missing optional fields such as the Realtime Database URL.
 */
public final class FirebaseBackendConfig {

    public static final String PROJECT_ID = "bitcamp26";
    public static final String FUNCTIONS_REGION = "us-central1";
    public static final String REALTIME_DATABASE_URL =
            "https://bitcamp26-default-rtdb.firebaseio.com";

    private FirebaseBackendConfig() {
    }
}
