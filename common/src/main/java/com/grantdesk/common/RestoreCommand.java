package com.grantdesk.common;

public record RestoreCommand(String drawId, String awardId, int amountCents) {
}
