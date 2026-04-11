package com.example.bitcamp26.data.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Repository responsible for basic Firebase authentication operations.
 */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;

    private static boolean localGuestSignedIn = false;
    private static String localGuestUserId = null;

    public AuthRepository() {
        this(FirebaseAuth.getInstance());
    }

    public AuthRepository(@NonNull FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    /**
     * Returns the currently signed-in Firebase user, or null if no user is signed in.
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Returns true if a Firebase user is signed in or a local guest session is active.
     */
    public boolean isSignedIn() {
        return getCurrentUser() != null
                || (localGuestSignedIn && localGuestUserId != null && !localGuestUserId.trim().isEmpty());
    }

    /**
     * Returns the current user's UID, or the local guest ID when fallback mode is active.
     */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return localGuestSignedIn ? localGuestUserId : null;
    }

    /**
     * Returns the current user's display name, or null if unavailable.
     */
    @Nullable
    public String getCurrentUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }

    @NonNull
    private String createLocalGuestUserId() {
        return "guest_" + System.currentTimeMillis();
    }

    /**
     * Signs in anonymously. If Firebase auth is unavailable or misconfigured,
     * falls back to a local guest session so frontend flows can still proceed.
     */
    public void signInAnonymously(@NonNull final AuthCallback callback) {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            localGuestSignedIn = false;
                            localGuestUserId = null;
                            callback.onSuccess(user);
                        } else {
                            localGuestSignedIn = true;
                            localGuestUserId = createLocalGuestUserId();
                            callback.onSuccess(null);
                        }
                    } else {
                        localGuestSignedIn = true;
                        localGuestUserId = createLocalGuestUserId();
                        callback.onSuccess(null);
                    }
                });
    }

    /**
     * Signs out the current user and clears any local guest session.
     */
    public void signOut() {
        firebaseAuth.signOut();
        localGuestSignedIn = false;
        localGuestUserId = null;
    }

    /**
     * Callback for auth results.
     */
    public interface AuthCallback {
        void onSuccess(@Nullable FirebaseUser user);
        void onError(@NonNull String errorMessage);
    }
}
