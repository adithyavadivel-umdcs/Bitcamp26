package com.example.bitcamp26.data.lobby;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Remote data source for reading and writing lobby data in Firebase Realtime Database.
 */
public class LobbyRemoteDataSource {

    private static final String LOBBIES_NODE = "lobbies";
    private static final long WRITE_TIMEOUT_MS = 10_000L;

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
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        boolean[] completed = {false};

        Runnable timeoutRunnable = () -> {
            if (!completed[0]) {
                completed[0] = true;
                callback.onError("Connection timed out. Check your network and try again.");
            }
        };
        handler.postDelayed(timeoutRunnable, WRITE_TIMEOUT_MS);

        lobbiesRef.child(lobbyCode).setValue(lobby)
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
                        callback.onError(getMessageOrDefault(e, "Failed to create lobby."));
                    }
                });
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

                    Lobby lobby = parseLobby(snapshot);
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
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        boolean[] completed = {false};

        Runnable timeoutRunnable = () -> {
            if (!completed[0]) {
                completed[0] = true;
                callback.onError("Connection timed out. Check your network and try again.");
            }
        };
        handler.postDelayed(timeoutRunnable, WRITE_TIMEOUT_MS);

        lobbiesRef.child(lobbyCode).setValue(lobby)
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
                        callback.onError(getMessageOrDefault(e, "Failed to update lobby."));
                    }
                });
    }

    /**
     * Updates only one player's ready field to avoid overwriting newer lobby changes from others.
     */
    public void updatePlayerReady(@NonNull String lobbyCode,
                                  @NonNull String playerId,
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

        lobbiesRef.child(lobbyCode).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                MutableData playersData = currentData.child("players");
                boolean foundPlayer = false;

                for (MutableData playerData : playersData.getChildren()) {
                    String snapshotPlayerId = playerData.child("id").getValue(String.class);
                    if (playerId.equals(snapshotPlayerId)) {
                        playerData.child("ready").setValue(ready);
                        foundPlayer = true;
                        break;
                    }
                }

                if (!foundPlayer) {
                    return Transaction.abort();
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error,
                                   boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (completed[0]) {
                    return;
                }

                completed[0] = true;
                handler.removeCallbacks(timeoutRunnable);

                if (error != null) {
                    callback.onError(error.getMessage() != null
                            ? error.getMessage()
                            : "Failed to update player readiness.");
                    return;
                }

                if (!committed) {
                    callback.onError("Current player was not found in the lobby.");
                    return;
                }

                callback.onSuccess();
            }
        });
    }

    /**
     * Updates every player's ready flag in one transaction for demo or bulk-ready flows.
     */
    public void updateAllPlayersReady(@NonNull String lobbyCode,
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

        lobbiesRef.child(lobbyCode).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                MutableData playersData = currentData.child("players");
                boolean foundPlayer = false;

                for (MutableData playerData : playersData.getChildren()) {
                    playerData.child("ready").setValue(ready);
                    foundPlayer = true;
                }

                if (!foundPlayer) {
                    return Transaction.abort();
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error,
                                   boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (completed[0]) {
                    return;
                }

                completed[0] = true;
                handler.removeCallbacks(timeoutRunnable);

                if (error != null) {
                    callback.onError(error.getMessage() != null
                            ? error.getMessage()
                            : "Failed to update ready states.");
                    return;
                }

                if (!committed) {
                    callback.onError("No players were found in the lobby.");
                    return;
                }

                callback.onSuccess();
            }
        });
    }

    /**
     * Updates a single player's location fields without replacing the entire lobby snapshot.
     */
    public void updatePlayerLocation(@NonNull String lobbyCode,
                                     @NonNull String playerId,
                                     double latitude,
                                     double longitude,
                                     long lastLocationUpdatedAt,
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

        lobbiesRef.child(lobbyCode).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                MutableData playersData = currentData.child("players");
                boolean foundPlayer = false;

                for (MutableData playerData : playersData.getChildren()) {
                    String snapshotPlayerId = playerData.child("id").getValue(String.class);
                    if (playerId.equals(snapshotPlayerId)) {
                        playerData.child("latitude").setValue(latitude);
                        playerData.child("longitude").setValue(longitude);
                        playerData.child("lastLocationUpdatedAt").setValue(lastLocationUpdatedAt);
                        foundPlayer = true;
                        break;
                    }
                }

                if (!foundPlayer) {
                    return Transaction.abort();
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error,
                                   boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (completed[0]) {
                    return;
                }

                completed[0] = true;
                handler.removeCallbacks(timeoutRunnable);

                if (error != null) {
                    callback.onError(error.getMessage() != null
                            ? error.getMessage()
                            : "Failed to update player location.");
                    return;
                }

                if (!committed) {
                    callback.onError("Current player was not found in the lobby.");
                    return;
                }

                callback.onSuccess();
            }
        });
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

                Lobby lobby = parseLobby(snapshot);
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

    private Lobby parseLobby(DataSnapshot snapshot) {
        Lobby lobby = new Lobby();
        lobby.setCode(snapshot.child("code").getValue(String.class));
        
        Boolean started = snapshot.child("started").getValue(Boolean.class);
        lobby.setStarted(started != null && started);
        
        Long maxPlayers = snapshot.child("maxPlayers").getValue(Long.class);
        lobby.setMaxPlayers(maxPlayers != null ? maxPlayers.intValue() : 0);
        
        Long duration = snapshot.child("matchDurationSeconds").getValue(Long.class);
        lobby.setMatchDurationSeconds(duration != null ? duration : 0L);
        
        Long playerCount = snapshot.child("playerCount").getValue(Long.class);
        lobby.setPlayerCount(playerCount != null ? playerCount.intValue() : 0);

        List<Player> players = new ArrayList<>();
        DataSnapshot playersSnapshot = snapshot.child("players");
        for (DataSnapshot playerSnapshot : playersSnapshot.getChildren()) {
            Player p = parsePlayer(playerSnapshot);
            if (p != null) {
                players.add(p);
            }
        }
        lobby.setPlayers(players);
        return lobby;
    }

    private Player parsePlayer(DataSnapshot snapshot) {
        Player player = new Player();
        player.setId(snapshot.child("id").getValue(String.class));
        player.setDisplayName(snapshot.child("displayName").getValue(String.class));
        
        Boolean ready = snapshot.child("ready").getValue(Boolean.class);
        player.setReady(ready != null && ready);
        
        Boolean caught = snapshot.child("caught").getValue(Boolean.class);
        player.setCaught(caught != null && caught);
        
        player.setCaughtBy(snapshot.child("caughtBy").getValue(String.class));
        
        Long caughtAt = snapshot.child("caughtAt").getValue(Long.class);
        player.setCaughtAt(caughtAt != null ? caughtAt : 0L);
        
        player.setCatchCode(snapshot.child("catchCode").getValue(String.class));

        String roleStr = snapshot.child("role").getValue(String.class);
        if (roleStr != null) {
            try {
                player.setRole(PlayerRole.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                player.setRole(null);
            }
        }

        Double lat = snapshot.child("latitude").getValue(Double.class);
        player.setLatitude(lat != null ? lat : 0.0);
        
        Double lon = snapshot.child("longitude").getValue(Double.class);
        player.setLongitude(lon != null ? lon : 0.0);
        
        Long lastUpdate = snapshot.child("lastLocationUpdatedAt").getValue(Long.class);
        player.setLastLocationUpdatedAt(lastUpdate != null ? lastUpdate : 0L);

        player.setHeldPowerup(parsePowerup(snapshot.child("heldPowerup")));
        player.setActivePowerup(parsePowerup(snapshot.child("activePowerup")));

        Long pActivatedAt = snapshot.child("powerupActivatedAt").getValue(Long.class);
        player.setPowerupActivatedAt(pActivatedAt != null ? pActivatedAt : 0L);
        
        Long pExpiresAt = snapshot.child("powerupExpiresAt").getValue(Long.class);
        player.setPowerupExpiresAt(pExpiresAt != null ? pExpiresAt : 0L);

        return player;
    }

    private PowerupType parsePowerup(DataSnapshot snapshot) {
        String typeStr = snapshot.getValue(String.class);
        if (typeStr == null) return null;
        try {
            return PowerupType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return null;
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
