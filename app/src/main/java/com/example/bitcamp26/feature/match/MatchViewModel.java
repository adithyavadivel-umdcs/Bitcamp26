
package com.example.bitcamp26.feature.match;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;
import com.example.bitcamp26.core.balance.GameBalance;
import com.example.bitcamp26.domain.usecase.ClaimHotspotUseCase;
import com.example.bitcamp26.domain.usecase.SubmitCatchCodeUseCase;
import com.example.bitcamp26.domain.usecase.SubmitLocationUseCase;
import com.example.bitcamp26.domain.usecase.UsePowerupUseCase;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the active match screen.
 *s
 * This class manages match state, selected player state, location submission,
 * hotspot claiming, catch code submission, and powerup usage.
 */
public class MatchViewModel extends ViewModel {

    private final SubmitLocationUseCase submitLocationUseCase;
    private final ClaimHotspotUseCase claimHotspotUseCase;
    private final SubmitCatchCodeUseCase submitCatchCodeUseCase;
    private final UsePowerupUseCase usePowerupUseCase;

    private final MutableLiveData<GameState> gameState = new MutableLiveData<>();
    private final MutableLiveData<Player> currentPlayer = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> insideHotspot = new MutableLiveData<>(false);
    private final MutableLiveData<HotspotState> claimableHotspot = new MutableLiveData<>();
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);

    public MatchViewModel() {
        this(
                new SubmitLocationUseCase(),
                new ClaimHotspotUseCase(),
                new SubmitCatchCodeUseCase(),
                new UsePowerupUseCase()
        );
    }

    public MatchViewModel(@NonNull SubmitLocationUseCase submitLocationUseCase,
                          @NonNull ClaimHotspotUseCase claimHotspotUseCase,
                          @NonNull SubmitCatchCodeUseCase submitCatchCodeUseCase,
                          @NonNull UsePowerupUseCase usePowerupUseCase) {
        this.submitLocationUseCase = submitLocationUseCase;
        this.claimHotspotUseCase = claimHotspotUseCase;
        this.submitCatchCodeUseCase = submitCatchCodeUseCase;
        this.usePowerupUseCase = usePowerupUseCase;
    }

    @NonNull
    public LiveData<GameState> getGameState() {
        return gameState;
    }

    @NonNull
    public LiveData<Player> getCurrentPlayer() {
        return currentPlayer;
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
    public LiveData<Boolean> getInsideHotspot() {
        return insideHotspot;
    }

    @NonNull
    public LiveData<HotspotState> getClaimableHotspot() {
        return claimableHotspot;
    }

    @NonNull
    public LiveData<Boolean> getGameFinished() {
        return gameFinished;
    }

    @NonNull
    public LiveData<Integer> getScore() {
        return score;
    }

    public void setInitialState(@Nullable GameState state,
                                @Nullable String currentPlayerId) {
        gameState.setValue(state);
        errorMessage.setValue(null);
        statusMessage.setValue(null);

        if (state == null) {
            currentPlayer.setValue(null);
            score.setValue(0);
            gameFinished.setValue(false);
            return;
        }

        score.setValue(state.getScore());
        gameFinished.setValue(state.isFinished());
        currentPlayer.setValue(findPlayerById(state.getPlayers(), currentPlayerId));
    }

    public void updateGameState(@Nullable GameState updatedState) {
        gameState.setValue(updatedState);

        if (updatedState == null) {
            score.setValue(0);
            gameFinished.setValue(false);
            insideHotspot.setValue(false);
            claimableHotspot.setValue(null);
            return;
        }

        score.setValue(updatedState.getScore());
        gameFinished.setValue(updatedState.isFinished());

        Player existingCurrentPlayer = currentPlayer.getValue();
        if (existingCurrentPlayer != null) {
            currentPlayer.setValue(findPlayerById(updatedState.getPlayers(), existingCurrentPlayer.getId()));
        }
    }

    public void submitLocation(@Nullable String playerId,
                               double latitude,
                               double longitude) {
        GameState state = gameState.getValue();
        loading.setValue(true);
        errorMessage.setValue(null);

        SubmitLocationUseCase.SubmitLocationResult result =
                submitLocationUseCase.execute(state, playerId, latitude, longitude);

        loading.setValue(false);

        if (!result.isSuccess()) {
            errorMessage.setValue(result.getMessage());
            return;
        }

        Player updatedPlayer = result.getUpdatedPlayer();
        if (updatedPlayer != null) {
            currentPlayer.setValue(updatedPlayer);
        }

        insideHotspot.setValue(result.isInsideHotspot());
        statusMessage.setValue("Location updated.");

        if (state != null && updatedPlayer != null) {
            HotspotState hotspot = claimHotspotUseCase.findClaimableHotspot(
                    state.getHotspots(),
                    updatedPlayer.getRole(),
                    latitude,
                    longitude
            );
            claimableHotspot.setValue(hotspot);
        } else {
            claimableHotspot.setValue(null);
        }
    }

    public void claimHotspot(@Nullable String playerId,
                             double latitude,
                             double longitude) {
        GameState state = gameState.getValue();
        Player player = currentPlayer.getValue();
        HotspotState hotspot = claimableHotspot.getValue();

        if (player == null && state != null) {
            player = findPlayerById(state.getPlayers(), playerId);
        }

        loading.setValue(true);
        errorMessage.setValue(null);

        ClaimHotspotUseCase.ClaimResult result = claimHotspotUseCase.execute(
                state,
                hotspot,
                playerId,
                player != null ? player.getRole() : null,
                latitude,
                longitude
        );

        loading.setValue(false);

        if (!result.isSuccess()) {
            errorMessage.setValue(result.getMessage());
            return;
        }

        if (state != null && result.getUpdatedHotspot() != null) {
            replaceHotspotInGameState(state, result.getUpdatedHotspot());
            state.setScore(result.getUpdatedScore());
            gameState.setValue(state);
            score.setValue(result.getUpdatedScore());
        }

        claimableHotspot.setValue(null);
        insideHotspot.setValue(false);
        statusMessage.setValue(result.getMessage());
    }

    public void submitCatchCode(@Nullable String submittingPlayerId,
                                @Nullable String targetPlayerId,
                                @Nullable String submittedCode) {
        GameState state = gameState.getValue();

        loading.setValue(true);
        errorMessage.setValue(null);

        SubmitCatchCodeUseCase.SubmitCatchCodeResult result =
                submitCatchCodeUseCase.execute(state, submittingPlayerId, targetPlayerId, submittedCode);

        loading.setValue(false);

        if (!result.isSuccess()) {
            errorMessage.setValue(result.getMessage());
            return;
        }

        if (state != null) {
            state.setScore(result.getUpdatedScore());
            state.setFinished(result.isGameFinished());
            gameState.setValue(state);
            score.setValue(result.getUpdatedScore());
            gameFinished.setValue(result.isGameFinished());
        }

        statusMessage.setValue(result.getMessage());
    }

    public void usePowerup(@Nullable String playerId,
                           @Nullable PowerupType powerupType) {
        GameState state = gameState.getValue();

        loading.setValue(true);
        errorMessage.setValue(null);

        UsePowerupUseCase.UsePowerupResult result =
                usePowerupUseCase.execute(state, playerId, powerupType);

        loading.setValue(false);

        if (!result.isSuccess()) {
            errorMessage.setValue(result.getMessage());
            return;
        }

        if (result.getUpdatedPlayer() != null) {
            currentPlayer.setValue(result.getUpdatedPlayer());
        }

        statusMessage.setValue(result.getMessage());
    }

    public void onShrinkTimerFired() {
        GameState state = gameState.getValue();
        if (state == null || state.isFinished()) return;

        // In a real app, this would call a Firebase Cloud Function to perform the shrink
        // on the server side. For this hackathon, we'll simulate the shrink locally
        // to keep the UI reactive.

        // Get current radius (simulated, usually part of GameState)
        // We'll use the GameBalance to calculate the shrink amount.
        // Since GameState doesn't store current radius yet, we'll assume DEFAULT for now.
        double currentRadius = GameBalance.DEFAULT_MAP_RADIUS_METERS;

        double shrinkAmount = GameBalance.shrinkAmountMeters(currentRadius);
        double newRadius = Math.max(GameBalance.minRadiusMeters(), currentRadius - shrinkAmount);

        statusMessage.setValue("Zone shrunk to " + (int) newRadius + "m!");

        // In a real implementation, we would update state.mapRadius and push to Firebase.
        // For now, we just notify the UI that the shrink event was processed.
    }

    @Nullable
    public Player getCurrentPlayerValue() {
        return currentPlayer.getValue();
    }

    @Nullable
    public GameState getGameStateValue() {
        return gameState.getValue();
    }

    @Nullable
    private Player findPlayerById(@Nullable List<Player> players,
                                  @Nullable String playerId) {
        if (players == null || players.isEmpty() || playerId == null || playerId.trim().isEmpty()) {
            return null;
        }

        for (Player player : players) {
            if (player == null || player.getId() == null) {
                continue;
            }
            if (playerId.equals(player.getId())) {
                return player;
            }
        }

        return null;
    }

    private void replaceHotspotInGameState(@NonNull GameState state,
                                           @NonNull HotspotState updatedHotspot) {
        List<HotspotState> existingHotspots = state.getHotspots();
        if (existingHotspots == null) {
            existingHotspots = new ArrayList<>();
            state.setHotspots(existingHotspots);
        }

        for (int i = 0; i < existingHotspots.size(); i++) {
            HotspotState hotspot = existingHotspots.get(i);
            if (hotspot == null || hotspot.getId() == null || updatedHotspot.getId() == null) {
                continue;
            }

            if (updatedHotspot.getId().equals(hotspot.getId())) {
                existingHotspots.set(i, updatedHotspot);
                return;
            }
        }

        existingHotspots.add(updatedHotspot);
    }
}
