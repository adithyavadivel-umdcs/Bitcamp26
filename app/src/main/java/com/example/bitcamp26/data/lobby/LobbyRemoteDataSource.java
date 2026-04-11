package com.example.bitcamp26.data.lobby;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Lobby;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Remote data source for reading and writing lobby data in Firebase Realtime Database.
 */
public class LobbyRemoteDataSource {

    private static final String LOBBIES_NODE = "lobbies";

    private final DatabaseReference lobbiesRef;

    public LobbyRemoteDataSource() {
        this(FirebaseDatabase.getInstance().getReference().child(LOBBIES_NODE));
    }

    public LobbyRemoteDataSource(@NonNull DatabaseReference lobbiesRef) {
        this.lobbiesRef = lobbiesRef;
    }

    /**
     * Creates or overwrites a lobby at /lobbies/{lobbyCode}.
     */
    public void createLobby(@NonNull String lobbyCode,
                            @NonNull Lobby lobby,
                            @NonNull final LobbyWriteCallback callback) {
        lobbiesRef.child(lobbyCode).setValue(lobby)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getMessageOrDefault(e, "Failed to create lobby.")));
    }

    /**
     * Reads a lobby once from Firebase.
     */
    public void fetchLobby(@NonNull String lobbyCode,
                           @NonNull final LobbyReadCallback callback) {
        lobbiesRef.child(lobbyCode)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError("Lobby not found.");
                        return;
                    }

                    Lobby lobby = snapshot.getValue(Lobby.class);
                    if (lobby == null) {
                        callback.onError("Lobby data is empty or malformed.");
                        return;
                    }

                    callback.onSuccess(lobby);
                })
                .addOnFailureListener(e -> callback.onError(getMessageOrDefault(e, "Failed to fetch lobby.")));
    }

    /**
     * Updates an existing lobby using setValue.
     */
    public void updateLobby(@NonNull String lobbyCode,
                            @NonNull Lobby lobby,
                            @NonNull final LobbyWriteCallback callback) {
        lobbiesRef.child(lobbyCode).setValue(lobby)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getMessageOrDefault(e, "Failed to update lobby.")));
    }

    /**
     * Deletes a lobby at /lobbies/{lobbyCode}.
     */
    public void deleteLobby(@NonNull String lobbyCode,
                            @NonNull final LobbyWriteCallback callback) {
        lobbiesRef.child(lobbyCode).removeValue()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getMessageOrDefault(e, "Failed to delete lobby.")));
    }

    /**
     * Attaches a realtime listener to a single lobby.
     * Remember to pass the returned listener to removeLobbyListener when no longer needed.
     */
    @NonNull
    public ValueEventListener listenToLobby(@NonNull String lobbyCode,
                                            @NonNull final LobbyReadCallback callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError("Lobby not found.");
                    return;
                }

                Lobby lobby = snapshot.getValue(Lobby.class);
                if (lobby == null) {
                    callback.onError("Lobby data is empty or malformed.");
                    return;
                }

                callback.onSuccess(lobby);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        lobbiesRef.child(lobbyCode).addValueEventListener(listener);
        return listener;
    }

    /**
     * Removes a previously attached realtime listener.
     */
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
