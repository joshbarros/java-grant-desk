package com.grantdesk.common;

public record ReserveCommand(String drawId, String awardId, int amountCents) {
}
