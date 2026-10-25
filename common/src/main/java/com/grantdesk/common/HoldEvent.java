package com.grantdesk.common;

public record HoldEvent(String drawId, String awardId, int amountCents, String status) {
}
