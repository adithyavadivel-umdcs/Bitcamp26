package com.example.bitcamp26.data.lobby;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Lobby;
import com.google.firebase.database.ValueEventListener;

/**
 * Repository layer for lobby operations.
 * Wraps the remote data source and provides a clean API for the rest of the app.
 */
public class LobbyRepository {

    private final LobbyRemoteDataSource remoteDataSource;

    public LobbyRepository() {
        this(new LobbyRemoteDataSource());
    }

    public LobbyRepository(@NonNull LobbyRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
    }

    /**
     * Creates a new lobby in Firebase.
     */
    public void createLobby(@NonNull String lobbyCode,
                            @NonNull Lobby lobby,
                            @NonNull final LobbyCallback callback) {
        remoteDataSource.createLobby(lobbyCode, lobby, new LobbyRemoteDataSource.LobbyWriteCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess(lobby);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Fetches a lobby once.
     */
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

    /**
     * Updates an existing lobby.
     */
    public void updateLobby(@NonNull String lobbyCode,
                            @NonNull Lobby lobby,
                            @NonNull final LobbyCallback callback) {
        remoteDataSource.updateLobby(lobbyCode, lobby, new LobbyRemoteDataSource.LobbyWriteCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess(lobby);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Updates only one player's ready flag without overwriting the full lobby.
     */
    public void updatePlayerReady(@NonNull String lobbyCode,
                                  @NonNull String playerId,
                                  boolean ready,
                                  @NonNull final SimpleCallback callback) {
        remoteDataSource.updatePlayerReady(lobbyCode, playerId, ready, new LobbyRemoteDataSource.LobbyWriteCallback() {
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

    /**
     * Updates every player's ready flag without replacing the full lobby snapshot.
     */
    public void updateAllPlayersReady(@NonNull String lobbyCode,
                                      boolean ready,
                                      @NonNull final SimpleCallback callback) {
        remoteDataSource.updateAllPlayersReady(lobbyCode, ready, new LobbyRemoteDataSource.LobbyWriteCallback() {
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

    /**
     * Updates only one player's location without replacing the full lobby snapshot.
     */
    public void updatePlayerLocation(@NonNull String lobbyCode,
                                     @NonNull String playerId,
                                     double latitude,
                                     double longitude,
                                     long lastLocationUpdatedAt,
                                     @NonNull final SimpleCallback callback) {
        remoteDataSource.updatePlayerLocation(
                lobbyCode,
                playerId,
                latitude,
                longitude,
                lastLocationUpdatedAt,
                new LobbyRemoteDataSource.LobbyWriteCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        callback.onError(errorMessage);
                    }
                }
        );
    }

    /**
     * Deletes a lobby.
     */
    public void deleteLobby(@NonNull String lobbyCode,
                            @NonNull final SimpleCallback callback) {
        remoteDataSource.deleteLobby(lobbyCode, new LobbyRemoteDataSource.LobbyWriteCallback() {
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

    /**
     * Starts listening to a lobby in real time.
     */
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

    /**
     * Stops listening to a lobby in real time.
     */
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
}
