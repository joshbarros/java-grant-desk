package com.grantdesk.common;

public record VoucherEvent(String drawId, int amountCents, String status) {
}
