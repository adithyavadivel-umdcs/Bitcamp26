package com.example.bitcamp26.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the business rules for joining a lobby.
 */
public class JoinLobbyUseCase {

    /**
     * Attempts to add a player to a lobby.
     */
    public JoinLobbyResult execute(@Nullable Lobby lobby,
                                   @Nullable Player player) {
        if (lobby == null) {
            return JoinLobbyResult.failure("Lobby is missing.");
        }

        if (player == null) {
            return JoinLobbyResult.failure("Player is missing.");
        }

        if (player.getId() == null || player.getId().trim().isEmpty()) {
            return JoinLobbyResult.failure("Player ID is missing.");
        }

        if (player.getDisplayName() == null || player.getDisplayName().trim().isEmpty()) {
            return JoinLobbyResult.failure("Player name is missing.");
        }

        if (isLobbyStarted(lobby)) {
            return JoinLobbyResult.failure("Cannot join because the match has already started.");
        }

        if (isLobbyFull(lobby)) {
            return JoinLobbyResult.failure("Lobby is full.");
        }

        if (containsPlayer(lobby, player.getId())) {
            return JoinLobbyResult.failure("Player is already in the lobby.");
        }

        List<Player> players = lobby.getPlayers();
        if (players == null) {
            players = new ArrayList<>();
            lobby.setPlayers(players);
        }

        if (player.getRole() == null) {
            player.setRole(assignRoleForNextPlayer(players));
        }

        players.add(player);
        lobby.setPlayerCount(players.size());

        return JoinLobbyResult.success(lobby, player);
    }

    /**
     * Returns true if the lobby has reached capacity.
     */
    public boolean isLobbyFull(@NonNull Lobby lobby) {
        int maxPlayers = lobby.getMaxPlayers();
        if (maxPlayers <= 0) {
            return false;
        }

        List<Player> players = lobby.getPlayers();
        int currentCount = players != null ? players.size() : 0;
        return currentCount >= maxPlayers;
    }

    /**
     * Returns true if the lobby has already started.
     */
    public boolean isLobbyStarted(@NonNull Lobby lobby) {
        return lobby.isStarted();
    }

    /**
     * Returns true if a player with the given ID is already in the lobby.
     */
    public boolean containsPlayer(@NonNull Lobby lobby,
                                  @Nullable String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return false;
        }

        List<Player> players = lobby.getPlayers();
        if (players == null || players.isEmpty()) {
            return false;
        }

        for (Player existingPlayer : players) {
            if (existingPlayer == null || existingPlayer.getId() == null) {
                continue;
            }

            if (playerId.equals(existingPlayer.getId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Simple role assignment rule: first player becomes SEEKER, others become HIDER.
     * Adjust later if your game design changes.
     */
    @NonNull
    public PlayerRole assignRoleForNextPlayer(@NonNull List<Player> currentPlayers) {
        return currentPlayers.isEmpty() ? PlayerRole.SEEKER : PlayerRole.HIDER;
    }

    public static final class JoinLobbyResult {
        private final boolean success;
        private final String message;
        private final Lobby updatedLobby;
        private final Player joinedPlayer;

        private JoinLobbyResult(boolean success,
                                @NonNull String message,
                                @Nullable Lobby updatedLobby,
                                @Nullable Player joinedPlayer) {
            this.success = success;
            this.message = message;
            this.updatedLobby = updatedLobby;
            this.joinedPlayer = joinedPlayer;
        }

        public static JoinLobbyResult success(@NonNull Lobby lobby,
                                              @NonNull Player player) {
            return new JoinLobbyResult(true, "Player joined lobby successfully.", lobby, player);
        }

        public static JoinLobbyResult failure(@NonNull String message) {
            return new JoinLobbyResult(false, message, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @Nullable
        public Lobby getUpdatedLobby() {
            return updatedLobby;
        }

        @Nullable
        public Player getJoinedPlayer() {
            return joinedPlayer;
        }
    }
}
