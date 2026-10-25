package com.grantdesk.common;

public record AwardOpened(String awardId, String sponsorId, String programId, String awardCode, int ceilingCents, int unitCents) {
}
