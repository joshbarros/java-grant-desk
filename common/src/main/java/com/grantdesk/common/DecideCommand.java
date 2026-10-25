package com.grantdesk.common;

public record DecideCommand(String drawId, String granteeCode, int amountCents) {
}
