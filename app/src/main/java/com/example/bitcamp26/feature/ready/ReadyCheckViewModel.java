package com.example.bitcamp26.feature.ready;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.data.lobby.LobbyRepository;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the RTDB-backed ready-check screen.
 */
public class ReadyCheckViewModel extends ViewModel {

    private final LobbyRepository lobbyRepository;

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

    private ValueEventListener activeLobbyListener;
    private String activeLobbyCode;

    public ReadyCheckViewModel() {
        this(new LobbyRepository());
    }

    public ReadyCheckViewModel(@NonNull LobbyRepository lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
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

    public void setLobby(@Nullable Lobby lobby) {
        currentLobby.setValue(lobby);
        recalculateDerivedState();
    }

    public void observeLobby(@Nullable String lobbyCode) {
        if (lobbyCode == null || lobbyCode.trim().isEmpty()) {
            errorMessage.setValue("Lobby code missing.");
            return;
        }

        clearActiveObserver();
        activeLobbyCode = lobbyCode.trim().toUpperCase();
        activeLobbyListener = lobbyRepository.observeLobby(activeLobbyCode, new LobbyRepository.LobbyCallback() {
            @Override
            public void onSuccess(@NonNull Lobby lobby) {
                currentLobby.postValue(lobby);
                recalculateDerivedState();
            }

            @Override
            public void onError(@NonNull String errorMessageValue) {
                errorMessage.postValue(errorMessageValue);
            }
        });
    }

    public void setCurrentPlayerId(@Nullable String playerId) {
        currentPlayerId.setValue(playerId);
        recalculateDerivedState();
    }

    public void toggleCurrentPlayerReady() {
        Lobby lobby = currentLobby.getValue();
        String playerId = currentPlayerId.getValue();
        Player player = lobby == null ? null : findCurrentPlayer(lobby);

        if (lobby == null || lobby.getCode() == null || lobby.getCode().trim().isEmpty()) {
            errorMessage.setValue("Cannot update readiness because the lobby is missing.");
            return;
        }
        if (playerId == null || playerId.trim().isEmpty() || player == null) {
            errorMessage.setValue("Current player was not found in the lobby.");
            return;
        }
        if (lobby.isStarted()) {
            errorMessage.setValue("Cannot change readiness after the match has started.");
            return;
        }

        loading.setValue(true);
        boolean nextReady = !player.isReady();
        lobbyRepository.setPlayerReady(lobby.getCode(), playerId, nextReady, new LobbyRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
                statusMessage.postValue(nextReady ? "You are marked ready." : "You are marked not ready.");
                errorMessage.postValue(null);
            }

            @Override
            public void onError(@NonNull String errorMessageValue) {
                loading.postValue(false);
                errorMessage.postValue(errorMessageValue);
            }
        });
    }

    public void markEveryoneReady() {
        errorMessage.setValue("Ready state is backend-owned per player. Each player must mark themselves ready.");
    }

    public void startMatch() {
        Lobby lobby = currentLobby.getValue();
        String playerId = currentPlayerId.getValue();
        if (lobby == null || lobby.getCode() == null || lobby.getCode().trim().isEmpty()) {
            errorMessage.setValue("Lobby missing. Cannot start the match.");
            return;
        }
        if (!Boolean.TRUE.equals(allPlayersReady.getValue())) {
            errorMessage.setValue("Not everyone is ready yet.");
            return;
        }
        if (playerId == null || !playerId.equals(lobby.getHostId())) {
            errorMessage.setValue("Only the host can start the match.");
            return;
        }

        loading.setValue(true);
        lobbyRepository.startGame(lobby.getCode(), new LobbyRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
                statusMessage.postValue("Match started.");
                errorMessage.postValue(null);
            }

            @Override
            public void onError(@NonNull String errorMessageValue) {
                loading.postValue(false);
                errorMessage.postValue(errorMessageValue);
            }
        });
    }

    @Override
    protected void onCleared() {
        clearActiveObserver();
        super.onCleared();
    }

    private void clearActiveObserver() {
        if (activeLobbyCode != null && activeLobbyListener != null) {
            lobbyRepository.removeLobbyObserver(activeLobbyCode, activeLobbyListener);
        }
        activeLobbyCode = null;
        activeLobbyListener = null;
    }

    private void recalculateDerivedState() {
        Lobby lobby = currentLobby.getValue();
        List<Player> players = getSafePlayers(lobby);

        int total = players.size();
        int ready = 0;
        for (Player player : players) {
            if (player != null && player.isReady()) {
                ready++;
            }
        }

        totalPlayerCount.postValue(total);
        readyPlayerCount.postValue(ready);
        readyProgressPercent.postValue(total == 0 ? 0 : (int) ((ready * 100f) / total));
        allPlayersReady.postValue(total > 0 && ready == total);
        currentPlayerReady.postValue(resolveCurrentPlayerReady(lobby));
        matchStarted.postValue(lobby != null && lobby.isStarted());
    }

    private boolean resolveCurrentPlayerReady(@Nullable Lobby lobby) {
        Player player = lobby == null ? null : findCurrentPlayer(lobby);
        return player != null && player.isReady();
    }

    @Nullable
    private Player findCurrentPlayer(@NonNull Lobby lobby) {
        String playerId = currentPlayerId.getValue();
        if (playerId == null || playerId.trim().isEmpty()) {
            return null;
        }

        for (Player player : getSafePlayers(lobby)) {
            if (player != null && playerId.equals(player.getId())) {
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
}
