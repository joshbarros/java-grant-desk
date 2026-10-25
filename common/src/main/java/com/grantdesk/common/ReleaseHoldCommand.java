package com.grantdesk.common;

public record ReleaseHoldCommand(String drawId, String awardId, int amountCents) {
}
