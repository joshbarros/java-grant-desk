package com.grantdesk.common;

public record PayCommand(String drawId, String awardId, int amountCents) {
}
