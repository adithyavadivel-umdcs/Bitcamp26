package com.example.bitcamp26.feature.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bitcamp26.data.auth.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel for the authentication screen.
 * Handles auth state, loading state, signed-in user, and error messages.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>(false);
    private final MutableLiveData<FirebaseUser> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel() {
        this(new AuthRepository());
    }

    public AuthViewModel(@NonNull AuthRepository authRepository) {
        this.authRepository = authRepository;
        refreshAuthState();
    }

    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    @NonNull
    public LiveData<Boolean> getSignedIn() {
        return signedIn;
    }

    @NonNull
    public LiveData<FirebaseUser> getCurrentUser() {
        return currentUser;
    }

    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Refreshes the current auth state from Firebase.
     */
    public void refreshAuthState() {
        FirebaseUser user = authRepository.getCurrentUser();
        currentUser.setValue(user);
        signedIn.setValue(user != null);
    }

    /**
     * Starts anonymous sign-in.
     */
    public void signInAnonymously() {
        loading.setValue(true);
        errorMessage.setValue(null);

        authRepository.signInAnonymously(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                loading.postValue(false);
                currentUser.postValue(user);
                signedIn.postValue(true);
                errorMessage.postValue(null);
            }

            @Override
            public void onError(@NonNull String error) {
                loading.postValue(false);
                currentUser.postValue(null);
                signedIn.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    /**
     * Signs out the current user and clears the local auth state.
     */
    public void signOut() {
        authRepository.signOut();
        currentUser.setValue(null);
        signedIn.setValue(false);
        loading.setValue(false);
    }

    /**
     * Returns the current Firebase user synchronously if available.
     */
    @Nullable
    public FirebaseUser peekCurrentUser() {
        return authRepository.getCurrentUser();
    }

    /**
     * Returns the current user's UID, or null if unavailable.
     */
    @Nullable
    public String getCurrentUserId() {
        return authRepository.getCurrentUserId();
    }
}
