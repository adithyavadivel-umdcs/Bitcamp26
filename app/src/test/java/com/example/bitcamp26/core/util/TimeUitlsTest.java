package com.example.bitcamp26.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeUitlsTest {

    @Test
    public void elapsedAndRemaining_areConsistentWithCurrentTime() {
        long now = TimeUitls.nowMillis();
        long future = now + 2_000;
        long past = now - 2_000;

        assertFalse(TimeUitls.hasElapsed(future));
        assertTrue(TimeUitls.hasElapsed(past));
        assertTrue(TimeUitls.millisRemaining(future) > 0);
        assertEquals(0L, TimeUitls.millisRemaining(past));
        assertTrue(TimeUitls.secondsRemaining(future) >= 1);
    }

    @Test
    public void minuteSecondFormatting_handlesBounds() {
        assertEquals("00:00", TimeUitls.formatMinutesSeconds(-1));
        assertEquals("01:05", TimeUitls.formatMinutesSeconds(65_000));
    }

    @Test
    public void humanFormatting_handlesSecondsMinutesAndHours() {
        assertEquals("45s", TimeUitls.formatDurationHuman(45_000));
        assertEquals("3m 12s", TimeUitls.formatDurationHuman(192_000));
        assertEquals("1h 05m", TimeUitls.formatDurationHuman(3_900_000));
    }

    @Test
    public void futureTimestampHelpers_returnReasonableValues() {
        long now = TimeUitls.nowMillis();
        long plusSeconds = TimeUitls.secondsFromNow(5);
        long plusMinutes = TimeUitls.minutesFromNow(2);

        assertTrue(plusSeconds >= now + 4_000);
        assertTrue(plusMinutes >= now + 119_000);
    }

    @Test
    public void timestampValidation_requiresPositiveValues() {
        assertFalse(TimeUitls.isValidTimestamp(0));
        assertFalse(TimeUitls.isValidTimestamp(-1));
        assertTrue(TimeUitls.isValidTimestamp(1));
    }
}
