package com.example.bitcamp26.domain.usecase;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.core.model.PlayerRole;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ClaimHotspotUseCaseTest {

    private final ClaimHotspotUseCase useCase = new ClaimHotspotUseCase();

    @Test
    public void executeRejectsInvalidInputsAndStates() {
        GameState gameState = new GameState();
        HotspotState hotspot = hotspot("hs1");

        assertFalse(useCase.execute(null, hotspot, "p1", PlayerRole.HIDER, 0, 0).isSuccess());
        assertFalse(useCase.execute(gameState, null, "p1", PlayerRole.HIDER, 0, 0).isSuccess());
        assertFalse(useCase.execute(gameState, hotspot, "", PlayerRole.HIDER, 0, 0).isSuccess());
        assertFalse(useCase.execute(gameState, hotspot, "p1", null, 0, 0).isSuccess());
    }

    @Test
    public void executeClaimsHotspotAndAwardsRoleSpecificScore() {
        GameState gameState = new GameState();
        gameState.setScore(5);
        HotspotState hotspot = hotspot("hs1");

        ClaimHotspotUseCase.ClaimResult result =
                useCase.execute(gameState, hotspot, "p1", PlayerRole.HIDER, 10.00001, 10.00001);

        assertTrue(result.isSuccess());
        assertEquals("p1", result.getUpdatedHotspot().getClaimedBy());
        assertFalse(result.getUpdatedHotspot().isActive());
        assertEquals(15, result.getUpdatedScore());
    }

    @Test
    public void helperMethodsIdentifyClaimableHotspots() {
        HotspotState claimable = hotspot("claimable");
        HotspotState claimed = hotspot("claimed");
        claimed.setClaimedBy("other");

        assertTrue(useCase.isPlayerAllowedToClaim(PlayerRole.HIDER));
        assertFalse(useCase.isPlayerAllowedToClaim(PlayerRole.UNASSIGNED));
        assertTrue(useCase.isWithinHotspotRadius(claimable, 10.00001, 10.00001));
        assertNull(useCase.findClaimableHotspot(Arrays.asList(claimed), PlayerRole.HIDER, 10, 10));
        assertEquals(claimable,
                useCase.findClaimableHotspot(Arrays.asList(claimed, claimable), PlayerRole.HIDER, 10.00001, 10.00001));
    }

    private HotspotState hotspot(String id) {
        HotspotState hotspot = new HotspotState();
        hotspot.setId(id);
        hotspot.setLat(10.0);
        hotspot.setLng(10.0);
        hotspot.setRadiusMeters(20.0);
        hotspot.setActive(true);
        return hotspot;
    }
}
