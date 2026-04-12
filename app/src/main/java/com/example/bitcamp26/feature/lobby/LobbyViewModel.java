package com.example.bitcamp26.feature.lobby;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.util.CodeUtils;
import com.example.bitcamp26.data.auth.AuthRepository;
import com.example.bitcamp26.data.lobby.LobbyRepository;
import com.google.firebase.database.ValueEventListener;

/**
 * ViewModel for backend-authoritative lobby creation, joining, and realtime observation.
 */
public class LobbyViewModel extends ViewModel {

    private final LobbyRepository lobbyRepository;
    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Lobby> currentLobby = new MutableLiveData<>();
    private final MutableLiveData<String> currentLobbyCode = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private ValueEventListener activeLobbyListener;
    private String activeObservedLobbyCode;

    public LobbyViewModel() {
        this(new LobbyRepository(), new AuthRepository());
    }

    public LobbyViewModel(@NonNull LobbyRepository lobbyRepository,
                          @NonNull AuthRepository authRepository) {
        this.lobbyRepository = lobbyRepository;
        this.authRepository = authRepository;
    }

    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    @NonNull
    public LiveData<Lobby> getCurrentLobby() {
        return currentLobby;
    }

    @NonNull
    public LiveData<String> getCurrentLobbyCode() {
        return currentLobbyCode;
    }

    @NonNull
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void createLobby(@Nullable String displayName) {
        String normalizedDisplayName = safeTrim(displayName);
        if (normalizedDisplayName.isEmpty()) {
            errorMessage.setValue("Please enter a display name.");
            return;
        }

        String userId = authRepository.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            errorMessage.setValue("You must be signed in before creating a lobby.");
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);

        lobbyRepository.createLobby(normalizedDisplayName, new LobbyRepository.LobbyCallback() {
            @Override
            public void onSuccess(@NonNull Lobby createdLobby) {
                String lobbyCode = createdLobby.getCode();
                loading.postValue(false);
                currentLobby.postValue(createdLobby);
                currentLobbyCode.postValue(lobbyCode);
                statusMessage.postValue("Lobby created: " + CodeUtils.formatCodeForDisplay(lobbyCode));
                errorMessage.postValue(null);
                observeLobby(lobbyCode);
            }

            @Override
            public void onError(@NonNull String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void joinLobby(@Nullable String rawLobbyCode,
                          @Nullable String displayName) {
        String normalizedDisplayName = safeTrim(displayName);
        if (normalizedDisplayName.isEmpty()) {
            errorMessage.setValue("Please enter a display name.");
            return;
        }

        String userId = authRepository.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            errorMessage.setValue("You must be signed in before joining a lobby.");
            return;
        }

        final String lobbyCode = CodeUtils.normalizeCode(rawLobbyCode);
        if (!CodeUtils.isValidCode(lobbyCode)) {
            errorMessage.setValue("Please enter a valid 6-character lobby code.");
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);

        lobbyRepository.joinLobby(lobbyCode, normalizedDisplayName, new LobbyRepository.LobbyCallback() {
            @Override
            public void onSuccess(@NonNull Lobby lobby) {
                loading.postValue(false);
                currentLobby.postValue(lobby);
                currentLobbyCode.postValue(lobby.getCode());
                statusMessage.postValue("Joined lobby: " + CodeUtils.formatCodeForDisplay(lobbyCode));
                errorMessage.postValue(null);
                observeLobby(lobbyCode);
            }

            @Override
            public void onError(@NonNull String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void observeLobby(@Nullable String rawLobbyCode) {
        String lobbyCode = CodeUtils.normalizeCode(rawLobbyCode);
        if (!CodeUtils.isValidCode(lobbyCode)) {
            errorMessage.setValue("Invalid lobby code for observation.");
            return;
        }

        clearActiveObserver();
        activeObservedLobbyCode = lobbyCode;

        activeLobbyListener = lobbyRepository.observeLobby(lobbyCode, new LobbyRepository.LobbyCallback() {
            @Override
            public void onSuccess(@NonNull Lobby lobby) {
                currentLobby.postValue(lobby);
                currentLobbyCode.postValue(lobbyCode);
            }

            @Override
            public void onError(@NonNull String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public void leaveObservedLobby() {
        clearActiveObserver();
        currentLobby.setValue(null);
        currentLobbyCode.setValue(null);
        statusMessage.setValue("Left lobby.");
    }

    @Nullable
    public String getSignedInUserId() {
        return authRepository.getCurrentUserId();
    }

    @Override
    protected void onCleared() {
        clearActiveObserver();
        super.onCleared();
    }

    private void clearActiveObserver() {
        if (activeObservedLobbyCode != null && activeLobbyListener != null) {
            lobbyRepository.removeLobbyObserver(activeObservedLobbyCode, activeLobbyListener);
        }
        activeObservedLobbyCode = null;
        activeLobbyListener = null;
    }

    @NonNull
    private String safeTrim(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
