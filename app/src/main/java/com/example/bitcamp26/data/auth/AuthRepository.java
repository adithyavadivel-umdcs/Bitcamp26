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
     * Returns true if a user is currently signed in.
     */
    public boolean isSignedIn() {
        return getCurrentUser() != null;
    }

    /**
     * Returns the current user's UID, or null if unavailable.
     */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Returns the current user's display name, or null if unavailable.
     */
    @Nullable
    public String getCurrentUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }

    /**
     * Signs in anonymously.
     */
    public void signInAnonymously(@NonNull final AuthCallback callback) {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onError("Sign-in succeeded, but no user was returned.");
                        }
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Anonymous sign-in failed.";
                        callback.onError(message);
                    }
                });
    }

    /**
     * Signs out the current user.
     */
    public void signOut() {
        firebaseAuth.signOut();
    }

    /**
     * Callback for auth results.
     */
    public interface AuthCallback {
        void onSuccess(@NonNull FirebaseUser user);
        void onError(@NonNull String errorMessage);
    }
}
