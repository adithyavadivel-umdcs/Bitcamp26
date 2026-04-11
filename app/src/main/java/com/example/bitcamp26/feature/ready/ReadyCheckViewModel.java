
package com.example.bitcamp26.feature.ready;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.domain.usecase.StartGameUseCase;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the ready-check screen.
 *
 * Responsibilities:
 * - hold the current lobby state during ready check
 * - track the current player ID
 * - expose derived state such as player list, readiness progress, and whether the match can start
 * - toggle the current player's ready state
 * - support a demo helper that marks everyone ready
 * - attempt to start a match through StartGameUseCase
 *
 * Notes:
 * - This ViewModel is designed to work even before a dedicated repository-backed ready system exists.
 * - Because the current Player model may not yet include a real `ready` boolean, this implementation
 *   stores readiness in the display name suffix "[READY]" as a temporary MVP strategy.
 * - Once the Player model adds a dedicated ready field, only `isPlayerReady(...)` and
 *   `applyReadyStateToPlayer(...)` need to be updated.
 */
public class ReadyCheckViewModel extends ViewModel {

    private final StartGameUseCase startGameUseCase;

    private final MutableLiveData<Lobby> currentLobby = new MutableLiveData<>();
    private final MutableLiveData<String> currentPlayerId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> readyPlayerCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalPlayerCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> readyProgressPercent = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> allPlayersReady = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> currentPlayerReady = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> matchStarted = new MutableLiveData<>(false);

    public ReadyCheckViewModel() {
        this(new StartGameUseCase());
    }

    public ReadyCheckViewModel(@NonNull StartGameUseCase startGameUseCase) {
        this.startGameUseCase = startGameUseCase;
    }

    @NonNull
    public LiveData<Lobby> getCurrentLobby() {
        return currentLobby;
    }

    @NonNull
    public LiveData<String> getCurrentPlayerId() {
        return currentPlayerId;
    }

    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    @NonNull
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    @NonNull
    public LiveData<Integer> getReadyPlayerCount() {
        return readyPlayerCount;
    }

    @NonNull
    public LiveData<Integer> getTotalPlayerCount() {
        return totalPlayerCount;
    }

    @NonNull
    public LiveData<Integer> getReadyProgressPercent() {
        return readyProgressPercent;
    }

    @NonNull
    public LiveData<Boolean> getAllPlayersReady() {
        return allPlayersReady;
    }

    @NonNull
    public LiveData<Boolean> getCurrentPlayerReady() {
        return currentPlayerReady;
    }

    @NonNull
    public LiveData<Boolean> getMatchStarted() {
        return matchStarted;
    }

    /**
     * Sets the current lobby and refreshes all derived state.
     */
    public void setLobby(@Nullable Lobby lobby) {
        currentLobby.setValue(lobby);
        recalculateDerivedState();
    }

    /**
     * Sets the current player ID and refreshes all derived state.
     */
    public void setCurrentPlayerId(@Nullable String playerId) {
        currentPlayerId.setValue(playerId);
        recalculateDerivedState();
    }

    /**
     * Recomputes all values derived from the lobby/player state.
     */
    public void refresh() {
        recalculateDerivedState();
    }

    /**
     * Toggles the current player's ready status.
     */
    public void toggleCurrentPlayerReady() {
        Lobby lobby = currentLobby.getValue();
        if (lobby == null) {
            errorMessage.setValue("Cannot update readiness because the lobby is missing.");
            return;
        }

        if (lobby.isStarted()) {
            errorMessage.setValue("Cannot change readiness after the match has started.");
            return;
        }

        Player player = findCurrentPlayer(lobby);
        if (player == null) {
            errorMessage.setValue("Current player was not found in the lobby.");
            return;
        }

        boolean nextReady = !isPlayerReady(player);
        applyReadyStateToPlayer(player, nextReady);

        currentLobby.setValue(lobby);
        statusMessage.setValue(nextReady ? "You are marked ready." : "You are marked not ready.");
        errorMessage.setValue(null);
        recalculateDerivedState();
    }

    /**
     * Demo helper to mark all players ready.
     */
    public void markEveryoneReady() {
        Lobby lobby = currentLobby.getValue();
        if (lobby == null || lobby.getPlayers() == null) {
            errorMessage.setValue("No lobby players available to update.");
            return;
        }

        for (Player player : lobby.getPlayers()) {
            if (player == null) {
                continue;
            }
            applyReadyStateToPlayer(player, true);
        }

        currentLobby.setValue(lobby);
        statusMessage.setValue("All players marked ready.");
        errorMessage.setValue(null);
        recalculateDerivedState();
    }

    /**
     * Attempts to start the match.
     *
     * This validates that everyone is ready, then delegates to StartGameUseCase.
     * Since ready check currently does not manage hotspots, an empty hotspot list is used.
     */
    public void startMatch() {
        Lobby lobby = currentLobby.getValue();
        if (lobby == null) {
            errorMessage.setValue("Lobby missing. Cannot start the match.");
            return;
        }

        if (lobby.isStarted()) {
            statusMessage.setValue("Match already started.");
            matchStarted.setValue(true);
            recalculateDerivedState();
            return;
        }

        if (!Boolean.TRUE.equals(allPlayersReady.getValue())) {
            errorMessage.setValue("Not everyone is ready yet.");
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);

        StartGameUseCase.StartGameResult result = startGameUseCase.execute(lobby, new ArrayList<>());

        loading.setValue(false);

        if (!result.isSuccess() || result.getUpdatedLobby() == null) {
            errorMessage.setValue(result.getMessage());
            return;
        }

        currentLobby.setValue(result.getUpdatedLobby());
        matchStarted.setValue(true);
        statusMessage.setValue(result.getMessage());
        errorMessage.setValue(null);
        recalculateDerivedState();
    }

    /**
     * Returns the current player from the current lobby, if present.
     */
    @Nullable
    public Player getCurrentPlayerValue() {
        Lobby lobby = currentLobby.getValue();
        return lobby == null ? null : findCurrentPlayer(lobby);
    }

    /**
     * Returns a safe copy of the current player list.
     */
    @NonNull
    public List<Player> getPlayersSnapshot() {
        Lobby lobby = currentLobby.getValue();
        if (lobby == null || lobby.getPlayers() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(lobby.getPlayers());
    }

    private void recalculateDerivedState() {
        Lobby lobby = currentLobby.getValue();
        List<Player> players = getSafePlayers(lobby);

        int total = players.size();
        int ready = 0;

        for (Player player : players) {
            if (player != null && isPlayerReady(player)) {
                ready++;
            }
        }

        totalPlayerCount.setValue(total);
        readyPlayerCount.setValue(ready);
        readyProgressPercent.setValue(total == 0 ? 0 : (int) ((ready * 100f) / total));
        allPlayersReady.setValue(total > 0 && ready == total);
        currentPlayerReady.setValue(resolveCurrentPlayerReady(lobby));
        matchStarted.setValue(lobby != null && lobby.isStarted());
    }

    private boolean resolveCurrentPlayerReady(@Nullable Lobby lobby) {
        Player player = lobby == null ? null : findCurrentPlayer(lobby);
        return player != null && isPlayerReady(player);
    }

    @Nullable
    private Player findCurrentPlayer(@NonNull Lobby lobby) {
        String playerId = currentPlayerId.getValue();
        if (playerId == null || playerId.trim().isEmpty()) {
            return null;
        }

        for (Player player : getSafePlayers(lobby)) {
            if (player == null || player.getId() == null) {
                continue;
            }
            if (playerId.equals(player.getId())) {
                return player;
            }
        }

        return null;
    }

    @NonNull
    private List<Player> getSafePlayers(@Nullable Lobby lobby) {
        if (lobby == null || lobby.getPlayers() == null) {
            return new ArrayList<>();
        }
        return lobby.getPlayers();
    }

    /**
     * Temporary MVP readiness rule.
     *
     * Replace this with `player.isReady()` once the Player model has a dedicated field.
     */
    private boolean isPlayerReady(@NonNull Player player) {
        String displayName = player.getDisplayName();
        return displayName != null && displayName.contains("[READY]");
    }

    /**
     * Temporary MVP readiness writer.
     *
     * Replace this with `player.setReady(ready)` once the Player model has a dedicated field.
     */
    private void applyReadyStateToPlayer(@NonNull Player player, boolean ready) {
        String name = player.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = "Player";
        }

        String cleaned = name.replace("[READY]", "").trim();
        player.setDisplayName(ready ? cleaned + " [READY]" : cleaned);
    }
}
