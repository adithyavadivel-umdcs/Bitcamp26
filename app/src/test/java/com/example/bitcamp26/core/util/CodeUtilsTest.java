package com.example.bitcamp26.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CodeUtilsTest {

    @Test
    public void generateCode_usesDefaultLengthAndAllowedCharacters() {
        String code = CodeUtils.generateCode();

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(CodeUtils.isValidCode(code));
    }

    @Test
    public void generateCode_invalidLengthFallsBackToDefault() {
        assertEquals(6, CodeUtils.generateCode(0).length());
        assertEquals(6, CodeUtils.generateCode(-5).length());
    }

    @Test
    public void normalizeCode_trimsRemovesSpacesAndUppercases() {
        assertEquals("ABC234", CodeUtils.normalizeCode("  ab c234 "));
        assertEquals("", CodeUtils.normalizeCode(null));
    }

    @Test
    public void isValidCode_enforcesLengthAndAlphabet() {
        assertTrue(CodeUtils.isValidCode("ABC234"));
        assertTrue(CodeUtils.isValidCode("abc234"));
        assertTrue(CodeUtils.isValidCode("ABCD2345", 8));
        assertFalse(CodeUtils.isValidCode("ABC23"));
        assertFalse(CodeUtils.isValidCode("ABC23I"));
        assertFalse(CodeUtils.isValidCode("ABC23O"));
        assertFalse(CodeUtils.isValidCode("ABC23!"));
        assertFalse(CodeUtils.isValidCode("ABCDEFG", 0));
    }

    @Test
    public void formatCodeForDisplay_splitsOnlyLongEnoughCodes() {
        assertEquals("ABC 234", CodeUtils.formatCodeForDisplay("abc234"));
        assertEquals("AB", CodeUtils.formatCodeForDisplay("ab"));
    }
}
