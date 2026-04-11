package com.example.bitcamp26.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.CodeUtils;
import com.example.bitcamp26.core.util.TimeUitls;

import java.util.List;

/**
 * Handles the business rules for submitting a catch code during a match.
 */
public class SubmitCatchCodeUseCase {

    /**
     * Validates a submitted catch code and updates the target player if successful.
     */
    public SubmitCatchCodeResult execute(@Nullable GameState gameState,
                                         @Nullable String submittingPlayerId,
                                         @Nullable String targetPlayerId,
                                         @Nullable String submittedCode) {
        if (gameState == null) {
            return SubmitCatchCodeResult.failure("Game state is missing.");
        }

        if (!gameState.isStarted()) {
            return SubmitCatchCodeResult.failure("The game has not started yet.");
        }

        if (gameState.isFinished()) {
            return SubmitCatchCodeResult.failure("The game is already over.");
        }

        if (submittingPlayerId == null || submittingPlayerId.trim().isEmpty()) {
            return SubmitCatchCodeResult.failure("Submitting player ID is missing.");
        }

        if (targetPlayerId == null || targetPlayerId.trim().isEmpty()) {
            return SubmitCatchCodeResult.failure("Target player ID is missing.");
        }

        if (submittingPlayerId.equals(targetPlayerId)) {
            return SubmitCatchCodeResult.failure("A player cannot catch themselves.");
        }

        String normalizedCode = CodeUtils.normalizeCode(submittedCode);
        if (!CodeUtils.isValidCode(normalizedCode)) {
            return SubmitCatchCodeResult.failure("Submitted catch code is invalid.");
        }

        Player submittingPlayer = findPlayerById(gameState.getPlayers(), submittingPlayerId);
        if (submittingPlayer == null) {
            return SubmitCatchCodeResult.failure("Submitting player was not found.");
        }

        Player targetPlayer = findPlayerById(gameState.getPlayers(), targetPlayerId);
        if (targetPlayer == null) {
            return SubmitCatchCodeResult.failure("Target player was not found.");
        }

        if (!isValidCatchRolePair(submittingPlayer.getRole(), targetPlayer.getRole())) {
            return SubmitCatchCodeResult.failure("This role combination is not allowed for a catch.");
        }

        if (targetPlayer.isCaught()) {
            return SubmitCatchCodeResult.failure("Target player has already been caught.");
        }

        String expectedCode = CodeUtils.normalizeCode(targetPlayer.getCatchCode());
        if (expectedCode.isEmpty()) {
            return SubmitCatchCodeResult.failure("Target player does not have a catch code assigned.");
        }

        if (!expectedCode.equals(normalizedCode)) {
            return SubmitCatchCodeResult.failure("Submitted catch code does not match.");
        }

        targetPlayer.setCaught(true);
        targetPlayer.setCaughtBy(submittingPlayerId);
        targetPlayer.setCaughtAt(TimeUitls.nowMillis());

        int updatedScore = calculateUpdatedScore(gameState.getScore(), submittingPlayer.getRole());
        gameState.setScore(updatedScore);

        if (areAllHidersCaught(gameState.getPlayers())) {
            gameState.setFinished(true);
        }

        return SubmitCatchCodeResult.success(targetPlayer, updatedScore, gameState.isFinished());
    }

    @Nullable
    public Player findPlayerById(@Nullable List<Player> players, @Nullable String playerId) {
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

    public boolean isValidCatchRolePair(@Nullable PlayerRole submittingRole,
                                        @Nullable PlayerRole targetRole) {
        return submittingRole == PlayerRole.SEEKER && targetRole == PlayerRole.HIDER;
    }

    public boolean areAllHidersCaught(@Nullable List<Player> players) {
        if (players == null || players.isEmpty()) {
            return false;
        }

        boolean foundHider = false;
        for (Player player : players) {
            if (player == null || player.getRole() == null) {
                continue;
            }

            if (player.getRole() == PlayerRole.HIDER) {
                foundHider = true;
                if (!player.isCaught()) {
                    return false;
                }
            }
        }

        return foundHider;
    }

    public int calculateUpdatedScore(int currentScore, @Nullable PlayerRole submittingRole) {
        if (submittingRole == PlayerRole.SEEKER) {
            return currentScore + 20;
        }
        return currentScore;
    }

    public static final class SubmitCatchCodeResult {
        private final boolean success;
        private final String message;
        private final Player updatedTargetPlayer;
        private final int updatedScore;
        private final boolean gameFinished;

        private SubmitCatchCodeResult(boolean success,
                                      @NonNull String message,
                                      @Nullable Player updatedTargetPlayer,
                                      int updatedScore,
                                      boolean gameFinished) {
            this.success = success;
            this.message = message;
            this.updatedTargetPlayer = updatedTargetPlayer;
            this.updatedScore = updatedScore;
            this.gameFinished = gameFinished;
        }

        public static SubmitCatchCodeResult success(@NonNull Player updatedTargetPlayer,
                                                    int updatedScore,
                                                    boolean gameFinished) {
            return new SubmitCatchCodeResult(
                    true,
                    "Catch code submitted successfully.",
                    updatedTargetPlayer,
                    updatedScore,
                    gameFinished
            );
        }

        public static SubmitCatchCodeResult failure(@NonNull String message) {
            return new SubmitCatchCodeResult(false, message, null, 0, false);
        }

        public boolean isSuccess() {
            return success;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @Nullable
        public Player getUpdatedTargetPlayer() {
            return updatedTargetPlayer;
        }

        public int getUpdatedScore() {
            return updatedScore;
        }

        public boolean isGameFinished() {
            return gameFinished;
        }
    }
}
