package com.example.bitcamp26.domain.usecase;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubmitCatchCodeUseCaseTest {

    private final SubmitCatchCodeUseCase useCase = new SubmitCatchCodeUseCase();

    @Test
    public void executeRejectsMissingIdsInvalidCodeAndBadState() {
        GameState state = gameState(true, false);
        assertFalse(useCase.execute(null, "s", "h", "ABC234").isSuccess());
        assertFalse(useCase.execute(gameState(false, false), "s", "h", "ABC234").isSuccess());
        assertFalse(useCase.execute(gameState(true, true), "s", "h", "ABC234").isSuccess());
        assertFalse(useCase.execute(state, "", "h", "ABC123").isSuccess());
        assertFalse(useCase.execute(state, "s", "", "ABC123").isSuccess());
        assertFalse(useCase.execute(state, "s", "h", "BAD!").isSuccess());
    }

    @Test
    public void executeCatchesTargetUpdatesScoreAndFinishesGameWhenAllHidersCaught() {
        GameState state = gameState(true, false);
        Player seeker = player("s", PlayerRole.SEEKER, false, null);
        Player target = player("h1", PlayerRole.HIDER, false, "ABC234");
        state.setPlayers(Arrays.asList(seeker, target));
        state.setScore(10);

        SubmitCatchCodeUseCase.SubmitCatchCodeResult result =
                useCase.execute(state, "s", "h1", "ABC234");

        assertTrue(result.isSuccess());
        assertTrue(result.getUpdatedTargetPlayer().isCaught());
        assertEquals("s", result.getUpdatedTargetPlayer().getCaughtBy());
        assertEquals(30, result.getUpdatedScore());
        assertTrue(result.isGameFinished());
    }

    @Test
    public void helperMethodsReturnExpectedValues() {
        Player seeker = player("s", PlayerRole.SEEKER, false, null);
        Player hider = player("h", PlayerRole.HIDER, false, "ABC234");

        assertEquals(seeker, useCase.findPlayerById(Arrays.asList(seeker, hider), "s"));
        assertTrue(useCase.isValidCatchRolePair(PlayerRole.SEEKER, PlayerRole.HIDER));
        assertFalse(useCase.isValidCatchRolePair(PlayerRole.HIDER, PlayerRole.SEEKER));
        assertFalse(useCase.areAllHidersCaught(Collections.singletonList(hider)));
        hider.setCaught(true);
        assertTrue(useCase.areAllHidersCaught(Collections.singletonList(hider)));
    }

    private GameState gameState(boolean started, boolean finished) {
        GameState gameState = new GameState();
        gameState.setStarted(started);
        gameState.setFinished(finished);
        return gameState;
    }

    private Player player(String id, PlayerRole role, boolean caught, String catchCode) {
        Player player = new Player();
        player.setId(id);
        player.setRole(role);
        player.setCaught(caught);
        player.setCatchCode(catchCode);
        return player;
    }
}
