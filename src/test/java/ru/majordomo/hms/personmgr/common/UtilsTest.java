package ru.majordomo.hms.personmgr.common;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest {
    @Test
    public void isSuitableVersion() {
        assertTrue(Utils.isSuitableVersion("*", null));
        assertTrue(Utils.isSuitableVersion("*", "any string"));
        assertTrue(Utils.isSuitableVersion("7.4", "7.4"));

        assertFalse(Utils.isSuitableVersion(null, "7.4"));
        assertFalse(Utils.isSuitableVersion(null, null));
        assertFalse(Utils.isSuitableVersion("7.1", null));
        assertFalse(Utils.isSuitableVersion("7.4", "7"));
    }
}
