package com.grantdesk.common;

public record DisbursementEvent(String drawId, int amountCents, String status) {
}
