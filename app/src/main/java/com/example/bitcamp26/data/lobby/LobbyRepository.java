package com.example.bitcamp26.data.lobby;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Lobby;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

/**
 * Repository for backend-authoritative lobby actions and RTDB observation.
 */
public class LobbyRepository {

    private final LobbyRemoteDataSource remoteDataSource;
    private final FunctionsRemoteDataSource functionsRemoteDataSource;

    public LobbyRepository() {
        this(new LobbyRemoteDataSource(), new FunctionsRemoteDataSource());
    }

    public LobbyRepository(@NonNull LobbyRemoteDataSource remoteDataSource,
                           @NonNull FunctionsRemoteDataSource functionsRemoteDataSource) {
        this.remoteDataSource = remoteDataSource;
        this.functionsRemoteDataSource = functionsRemoteDataSource;
    }

    public void createLobby(@NonNull String displayName,
                            @NonNull final LobbyCallback callback) {
        functionsRemoteDataSource.callCreateLobby(displayName, new FunctionsRemoteDataSource.FunctionCallback() {
            @Override
            public void onSuccess(@NonNull Map<String, Object> result) {
                String lobbyId = getString(result.get("lobbyId"));
                if (lobbyId == null || lobbyId.trim().isEmpty()) {
                    callback.onError("Backend did not return a lobby ID.");
                    return;
                }
                getLobby(lobbyId, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(getMessageOrDefault(e, "Failed to create lobby."));
            }
        });
    }

    public void joinLobby(@NonNull String inviteCode,
                          @NonNull String displayName,
                          @NonNull final LobbyCallback callback) {
        functionsRemoteDataSource.callJoinLobby(inviteCode, displayName, new FunctionsRemoteDataSource.FunctionCallback() {
            @Override
            public void onSuccess(@NonNull Map<String, Object> result) {
                String lobbyId = getString(result.get("lobbyId"));
                if (lobbyId == null || lobbyId.trim().isEmpty()) {
                    callback.onError("Backend did not return a lobby ID.");
                    return;
                }
                getLobby(lobbyId, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(getMessageOrDefault(e, "Failed to join lobby."));
            }
        });
    }

    public void getLobby(@NonNull String lobbyCode,
                         @NonNull final LobbyCallback callback) {
        remoteDataSource.fetchLobby(lobbyCode, new LobbyRemoteDataSource.LobbyReadCallback() {
            @Override
            public void onSuccess(@NonNull Lobby lobby) {
                callback.onSuccess(lobby);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void setPlayerReady(@NonNull String lobbyCode,
                               @NonNull String userId,
                               boolean ready,
                               @NonNull final SimpleCallback callback) {
        remoteDataSource.updatePlayerReady(lobbyCode, userId, ready, new LobbyRemoteDataSource.LobbyWriteCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void startGame(@NonNull String lobbyCode,
                          @NonNull final SimpleCallback callback) {
        functionsRemoteDataSource.callStartGame(lobbyCode, new FunctionsRemoteDataSource.FunctionCallback() {
            @Override
            public void onSuccess(@NonNull Map<String, Object> result) {
                callback.onSuccess();
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(getMessageOrDefault(e, "Failed to start game."));
            }
        });
    }

    @NonNull
    public ValueEventListener observeLobby(@NonNull String lobbyCode,
                                           @NonNull final LobbyCallback callback) {
        return remoteDataSource.listenToLobby(lobbyCode, new LobbyRemoteDataSource.LobbyReadCallback() {
            @Override
            public void onSuccess(@NonNull Lobby lobby) {
                callback.onSuccess(lobby);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void removeLobbyObserver(@NonNull String lobbyCode,
                                    @Nullable ValueEventListener listener) {
        remoteDataSource.removeLobbyListener(lobbyCode, listener);
    }

    public interface LobbyCallback {
        void onSuccess(@NonNull Lobby lobby);
        void onError(@NonNull String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(@NonNull String errorMessage);
    }

    @Nullable
    private String getString(@Nullable Object value) {
        return value instanceof String ? (String) value : null;
    }

    @NonNull
    private String getMessageOrDefault(@Nullable Exception e, @NonNull String defaultMessage) {
        return e != null && e.getMessage() != null && !e.getMessage().trim().isEmpty()
                ? e.getMessage()
                : defaultMessage;
    }
}
