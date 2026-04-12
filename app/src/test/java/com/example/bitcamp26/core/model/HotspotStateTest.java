package com.example.bitcamp26.core.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HotspotStateTest {

    @Test
    public void constructorInitializesReusableActiveHotspot() {
        HotspotState hotspot = new HotspotState("hs1", 1.0, 2.0, 3.0, "SEEKER_MINIMAP_BOOST");

        assertEquals("hs1", hotspot.getId());
        assertTrue(hotspot.isActive());
        assertNull(hotspot.getClaimedBy());
        assertNull(hotspot.getClaimedAt());
    }

    @Test
    public void setPowerupTypeStoresEnumNameAndParsesItBack() {
        HotspotState hotspot = new HotspotState();
        hotspot.setPowerupType(PowerupType.HIDER_VISION_REDUCTION);

        assertEquals("HIDER_VISION_REDUCTION", hotspot.getPowerupTypeString());
        assertEquals(PowerupType.HIDER_VISION_REDUCTION, hotspot.getPowerupType());
    }
}
