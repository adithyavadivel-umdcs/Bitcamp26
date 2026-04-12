package com.example.bitcamp26.core.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerModelTest {

    @Test
    public void idAndUserIdStayInSync() {
        Player player = new Player();

        player.setId("alpha");
        assertEquals("alpha", player.getId());
        assertEquals("alpha", player.getUserId());

        player.setUserId("beta");
        assertEquals("beta", player.getId());
        assertEquals("beta", player.getUserId());
    }

    @Test
    public void caughtAndAliveMirrorEachOther() {
        Player player = new Player();

        player.setCaught(true);
        assertTrue(player.isCaught());
        assertFalse(player.isAlive());

        player.setAlive(true);
        assertFalse(player.isCaught());
        assertTrue(player.isAlive());
    }

    @Test
    public void powerupFieldsDefaultToNoneAndAcceptUpdates() {
        Player player = new Player();
        assertEquals(PowerupType.NONE, player.getHeldPowerup());
        assertEquals(PowerupType.NONE, player.getActivePowerup());

        player.setHeldPowerup(PowerupType.HIDER_VISION_REDUCTION);
        player.setActivePowerup(PowerupType.SEEKER_MINIMAP_BOOST);

        assertEquals(PowerupType.HIDER_VISION_REDUCTION, player.getHeldPowerup());
        assertEquals(PowerupType.SEEKER_MINIMAP_BOOST, player.getActivePowerup());
    }
}
