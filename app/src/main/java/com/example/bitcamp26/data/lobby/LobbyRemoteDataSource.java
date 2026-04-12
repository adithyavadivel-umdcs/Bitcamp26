package com.example.bitcamp26.data.lobby;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.util.FirebaseBackendConfig;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * RTDB data source for observing backend-authoritative lobby state and writing
 * player-owned fields such as ready.
 */
public class LobbyRemoteDataSource {

    private static final String LOBBIES_NODE = "lobbies";
    private static final long WRITE_TIMEOUT_MS = 10_000L;

    private final DatabaseReference lobbiesRef;

    public LobbyRemoteDataSource() {
        this(FirebaseDatabase.getInstance(FirebaseBackendConfig.REALTIME_DATABASE_URL)
                .getReference()
                .child(LOBBIES_NODE));
    }

    public LobbyRemoteDataSource(@NonNull DatabaseReference lobbiesRef) {
        this.lobbiesRef = lobbiesRef;
    }

    public void fetchLobby(@NonNull String lobbyCode,
                           @NonNull final LobbyReadCallback callback) {
        lobbiesRef.child(lobbyCode)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError("Lobby not found.");
                        return;
                    }
                    callback.onSuccess(LobbySnapshotMapper.fromSnapshot(snapshot));
                })
                .addOnFailureListener(e -> callback.onError(getMessageOrDefault(e, "Failed to fetch lobby.")));
    }

    public void updatePlayerReady(@NonNull String lobbyCode,
                                  @NonNull String userId,
                                  boolean ready,
                                  @NonNull final LobbyWriteCallback callback) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        boolean[] completed = {false};

        Runnable timeoutRunnable = () -> {
            if (!completed[0]) {
                completed[0] = true;
                callback.onError("Connection timed out. Check your network and try again.");
            }
        };
        handler.postDelayed(timeoutRunnable, WRITE_TIMEOUT_MS);

        lobbiesRef.child(lobbyCode)
                .child("players")
                .child(userId)
                .child("ready")
                .setValue(ready)
                .addOnSuccessListener(unused -> {
                    if (!completed[0]) {
                        completed[0] = true;
                        handler.removeCallbacks(timeoutRunnable);
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!completed[0]) {
                        completed[0] = true;
                        handler.removeCallbacks(timeoutRunnable);
                        callback.onError(getMessageOrDefault(e, "Failed to update ready state."));
                    }
                });
    }

    @NonNull
    public ValueEventListener listenToLobby(@NonNull String lobbyCode,
                                            @NonNull final LobbyReadCallback callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError("Lobby not found.");
                    return;
                }

                callback.onSuccess(LobbySnapshotMapper.fromSnapshot(snapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        lobbiesRef.child(lobbyCode).addValueEventListener(listener);
        return listener;
    }

    public void removeLobbyListener(@NonNull String lobbyCode,
                                    @Nullable ValueEventListener listener) {
        if (listener != null) {
            lobbiesRef.child(lobbyCode).removeEventListener(listener);
        }
    }

    @NonNull
    private String getMessageOrDefault(@Nullable Exception e, @NonNull String defaultMessage) {
        return e != null && e.getMessage() != null && !e.getMessage().trim().isEmpty()
                ? e.getMessage()
                : defaultMessage;
    }

    public interface LobbyReadCallback {
        void onSuccess(@NonNull Lobby lobby);
        void onError(@NonNull String errorMessage);
    }

    public interface LobbyWriteCallback {
        void onSuccess();
        void onError(@NonNull String errorMessage);
    }
}
