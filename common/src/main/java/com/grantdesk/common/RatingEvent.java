package com.grantdesk.common;

public record RatingEvent(String drawId, int amountCents, String status, String reason) {
}
