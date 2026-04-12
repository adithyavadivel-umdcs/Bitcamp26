package com.example.bitcamp26.domain.usecase;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubmitLocationUseCaseTest {

    private final SubmitLocationUseCase useCase = new SubmitLocationUseCase();

    @Test
    public void executeRejectsMissingOrInactiveGameState() {
        assertFalse(useCase.execute(null, "p1", 1, 1).isSuccess());
        assertFalse(useCase.execute(gameState(false, false), "p1", 1, 1).isSuccess());
        assertFalse(useCase.execute(gameState(true, true), "p1", 1, 1).isSuccess());
    }

    @Test
    public void executeRejectsInvalidPlayerOrCoordinates() {
        GameState state = gameState(true, false);
        Player player = player("p1", PlayerRole.HIDER, false);
        state.setPlayers(Collections.singletonList(player));

        assertFalse(useCase.execute(state, "", 1, 1).isSuccess());
        assertFalse(useCase.execute(state, "missing", 1, 1).isSuccess());
        assertFalse(useCase.execute(state, "p1", 1000, 1).isSuccess());
    }

    @Test
    public void executeUpdatesLocationAndDetectsHotspotOnSuccess() {
        GameState state = gameState(true, false);
        Player player = player("p1", PlayerRole.HIDER, false);

        HotspotState hotspot = new HotspotState();
        hotspot.setId("hs1");
        hotspot.setLat(10.0);
        hotspot.setLng(10.0);
        hotspot.setRadiusMeters(20.0);
        hotspot.setActive(true);

        state.setPlayers(Collections.singletonList(player));
        state.setHotspots(Collections.singletonList(hotspot));

        SubmitLocationUseCase.SubmitLocationResult result =
                useCase.execute(state, "p1", 10.00005, 10.00005);

        assertTrue(result.isSuccess());
        assertTrue(result.isInsideHotspot());
        assertEquals(10.00005, result.getUpdatedPlayer().getLatitude(), 0.000001);
        assertTrue(result.getUpdatedPlayer().getLastLocationUpdatedAt() > 0);
    }

    @Test
    public void helperMethodsRespectCurrentRoleRules() {
        assertTrue(useCase.canRoleSubmitLocation(PlayerRole.SEEKER));
        assertTrue(useCase.canRoleSubmitLocation(PlayerRole.HIDER));
        assertFalse(useCase.canRoleSubmitLocation(PlayerRole.UNASSIGNED));
    }

    private GameState gameState(boolean started, boolean finished) {
        GameState gameState = new GameState();
        gameState.setStarted(started);
        gameState.setFinished(finished);
        return gameState;
    }

    private Player player(String id, PlayerRole role, boolean caught) {
        Player player = new Player();
        player.setId(id);
        player.setRole(role);
        player.setCaught(caught);
        return player;
    }
}
