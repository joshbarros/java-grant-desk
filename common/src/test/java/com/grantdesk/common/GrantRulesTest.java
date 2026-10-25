package com.grantdesk.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrantRulesTest {

    @Test
    void unitsStayOnTheBound() {
        assertTrue(GrantRules.emptyUnits(0));
        assertFalse(GrantRules.emptyUnits(1));
        assertFalse(GrantRules.overUnits(80));
        assertTrue(GrantRules.overUnits(81));
    }

    @Test
    void amountStaysInsideTheIntegerRange() {
        assertEquals(1000, GrantRules.amount(8, 125));
        assertEquals(10000, GrantRules.amount(80, 125));
        assertTrue(GrantRules.fits(5000, 1000));
        assertFalse(GrantRules.fits(500, 1000));
        assertFalse(GrantRules.fits(5000, 0));
        assertThrows(ArithmeticException.class, () -> GrantRules.amount(2, Integer.MAX_VALUE));
    }

    @Test
    void lapsedGranteesMatchTheCode() {
        assertTrue(GrantRules.lapsedGrantee("G-lapse"));
        assertTrue(GrantRules.lapsedGrantee("unlapsed"));
        assertFalse(GrantRules.lapsedGrantee("G-1"));
    }
}
