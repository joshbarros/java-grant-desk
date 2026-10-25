package com.grantdesk.common;

import java.util.Locale;

public final class GrantRules {
    public static final int OVER_UNITS = 80;

    private GrantRules() {
    }

    public static boolean emptyUnits(int units) {
        return units == 0;
    }

    public static boolean overUnits(int units) {
        return units > OVER_UNITS;
    }

    public static int amount(int units, int unitCents) {
        return Math.multiplyExact(units, unitCents);
    }

    public static boolean fits(int openCents, int amountCents) {
        return amountCents > 0 && amountCents <= openCents;
    }

    public static boolean lapsedGrantee(String granteeCode) {
        return granteeCode != null && granteeCode.toLowerCase(Locale.ROOT).contains("lapse");
    }
}
