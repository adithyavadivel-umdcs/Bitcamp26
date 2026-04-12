package com.example.bitcamp26.domain.usecase;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UsePowerupUseCaseTest {

    private final UsePowerupUseCase useCase = new UsePowerupUseCase();

    @Test
    public void executeRejectsMissingStatePlayerOrPowerup() {
        assertFalse(useCase.execute(null, "p1", PowerupType.HIDER_VISION_REDUCTION).isSuccess());
        assertFalse(useCase.execute(gameState(false, false), "p1", PowerupType.HIDER_VISION_REDUCTION).isSuccess());
        assertFalse(useCase.execute(gameState(true, true), "p1", PowerupType.HIDER_VISION_REDUCTION).isSuccess());
        assertFalse(useCase.execute(gameState(true, false), "", PowerupType.HIDER_VISION_REDUCTION).isSuccess());
        assertFalse(useCase.execute(gameState(true, false), "p1", null).isSuccess());
    }

    @Test
    public void executeActivatesPowerupAndClearsInventory() {
        GameState state = gameState(true, false);
        Player player = player("p1", PlayerRole.SEEKER, false, PowerupType.SEEKER_MINIMAP_BOOST);
        state.setPlayers(Collections.singletonList(player));

        UsePowerupUseCase.UsePowerupResult result =
                useCase.execute(state, "p1", PowerupType.SEEKER_MINIMAP_BOOST);

        assertTrue(result.isSuccess());
        assertEquals(PowerupType.SEEKER_MINIMAP_BOOST, result.getActivatedPowerup());
        assertEquals(PowerupType.SEEKER_MINIMAP_BOOST, result.getUpdatedPlayer().getActivePowerup());
        assertEquals(PowerupType.NONE, result.getUpdatedPlayer().getHeldPowerup());
        assertTrue(result.getPowerupExpiresAt() > result.getUpdatedPlayer().getPowerupActivatedAt());
    }

    @Test
    public void helperMethodsRespectCurrentRoleAndDurationRules() {
        Player hider = player("h", PlayerRole.HIDER, false, PowerupType.HIDER_VISION_REDUCTION);

        assertEquals(hider, useCase.findPlayerById(Collections.singletonList(hider), "h"));
        assertTrue(useCase.isPowerupAllowedForRole(PlayerRole.HIDER, PowerupType.HIDER_VISION_REDUCTION));
        assertTrue(useCase.isPowerupAllowedForRole(PlayerRole.SEEKER, PowerupType.SEEKER_MINIMAP_BOOST));
        assertFalse(useCase.isPowerupAllowedForRole(PlayerRole.HIDER, PowerupType.SEEKER_MINIMAP_BOOST));
        assertTrue(useCase.hasMatchingInventoryPowerup(hider, PowerupType.HIDER_VISION_REDUCTION));
        assertEquals(20_000L, useCase.resolveEffectDurationMillis(PowerupType.HIDER_VISION_REDUCTION));
        assertEquals(15_000L, useCase.resolveEffectDurationMillis(PowerupType.SEEKER_MINIMAP_BOOST));
    }

    private GameState gameState(boolean started, boolean finished) {
        GameState state = new GameState();
        state.setStarted(started);
        state.setFinished(finished);
        return state;
    }

    private Player player(String id, PlayerRole role, boolean caught, PowerupType heldPowerup) {
        Player player = new Player();
        player.setId(id);
        player.setRole(role);
        player.setCaught(caught);
        player.setHeldPowerup(heldPowerup);
        return player;
    }
}
