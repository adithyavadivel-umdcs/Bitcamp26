package com.example.bitcamp26.feature.match.components;

import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerMarkersRendererTest {

    private final PlayerMarkersRenderer renderer = new PlayerMarkersRenderer();

    @Test
    public void buildMarkersSkipsPlayersWithoutRenderableCoordinates() {
        Player valid = player("p1", PlayerRole.HIDER, 10, 10);
        Player invalid = player("p2", PlayerRole.HIDER, 1000, 10);

        List<PlayerMarkersRenderer.RenderablePlayerMarker> markers =
                renderer.buildMarkers(Arrays.asList(valid, invalid), "viewer", PlayerRole.HIDER, false, true);

        assertEquals(1, markers.size());
        assertEquals("p1", markers.get(0).getPlayerId());
    }

    @Test
    public void seekerOnlySeesHidersWhenExpandedVisionIsEnabled() {
        Player seeker = player("s", PlayerRole.SEEKER, 10, 10);
        Player hider = player("h", PlayerRole.HIDER, 10.1, 10.1);

        assertFalse(renderer.shouldRenderPlayer(hider, "s", PlayerRole.SEEKER, false, true));
        assertTrue(renderer.shouldRenderPlayer(hider, "s", PlayerRole.SEEKER, true, true));
        assertTrue(renderer.shouldRenderPlayer(seeker, "s", PlayerRole.SEEKER, false, true));
    }

    @Test
    public void markerStyleAndLabelsReflectPlayerState() {
        Player self = player("self", PlayerRole.HIDER, 10, 10);
        self.setHeldPowerup(PowerupType.HIDER_VISION_REDUCTION);
        Player caught = player("caught", PlayerRole.HIDER, 11, 11);
        caught.setCaught(true);

        PlayerMarkersRenderer.RenderablePlayerMarker selfMarker =
                renderer.toRenderableMarker(self, "self");
        PlayerMarkersRenderer.RenderablePlayerMarker caughtMarker =
                renderer.toRenderableMarker(caught, "self");

        assertEquals("Player (You)", selfMarker.getLabel());
        assertEquals(PlayerMarkersRenderer.MarkerStyle.SELF, selfMarker.getMarkerStyle());
        assertTrue(selfMarker.getSubtitle().contains("SELF"));
        assertEquals(PlayerMarkersRenderer.MarkerStyle.CAUGHT, caughtMarker.getMarkerStyle());
        assertTrue(caughtMarker.getSubtitle().contains("CAUGHT"));
    }

    @Test
    public void hiderVisionReductionKeepsHiderHiddenWithoutExpandedVision() {
        Player hiddenHider = player("h1", PlayerRole.HIDER, 10, 10);
        hiddenHider.setActivePowerup(PowerupType.HIDER_VISION_REDUCTION);

        assertFalse(renderer.shouldRenderPlayer(hiddenHider, "s", PlayerRole.SEEKER, false, true));
    }

    private Player player(String id, PlayerRole role, double lat, double lng) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName("Player");
        player.setRole(role);
        player.setLatitude(lat);
        player.setLongitude(lng);
        return player;
    }
}
