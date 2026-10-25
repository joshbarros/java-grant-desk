package com.grantdesk.common;

public final class Channels {
    public static final String SPONSORS = "grant.sponsors";
    public static final String PROGRAMS = "grant.programs";
    public static final String GRANTEES = "grant.grantees";
    public static final String MILESTONES = "grant.milestones";
    public static final String AWARDS = "grant.awards";
    public static final String DRAWS = "grant.draws";
    public static final String RATINGS = "grant.ratings";
    public static final String DECISIONS = "grant.decisions";
    public static final String HOLDS = "grant.holds";
    public static final String VOUCHERS = "grant.vouchers";
    public static final String DISBURSEMENTS = "grant.disbursements";
    public static final String ACKS = "grant.acknowledgements";
    public static final String CLAWBACKS = "grant.clawbacks";

    public static final String RATE = "grant.eligibility.rate";
    public static final String RATE_DLQ = "grant.eligibility.rate.dlq";
    public static final String RESERVE = "grant.award.reserve";
    public static final String RESERVE_DLQ = "grant.award.reserve.dlq";
    public static final String DECIDE = "grant.compliance.decide";
    public static final String DECIDE_DLQ = "grant.compliance.decide.dlq";
    public static final String RELEASE = "grant.clawback.release";
    public static final String RELEASE_DLQ = "grant.clawback.release.dlq";
    public static final String RELEASE_HOLD = "grant.award.release";
    public static final String RELEASE_HOLD_DLQ = "grant.award.release.dlq";
    public static final String CUT = "grant.voucher.cut";
    public static final String CUT_DLQ = "grant.voucher.cut.dlq";
    public static final String PAY = "grant.award.pay";
    public static final String PAY_DLQ = "grant.award.pay.dlq";
    public static final String WIRE = "grant.disbursement.wire";
    public static final String WIRE_DLQ = "grant.disbursement.wire.dlq";
    public static final String ACK = "grant.acknowledgement.issue";
    public static final String ACK_DLQ = "grant.acknowledgement.issue.dlq";
    public static final String RETURN = "grant.clawback.return";
    public static final String RETURN_DLQ = "grant.clawback.return.dlq";
    public static final String RESTORE = "grant.award.restore";
    public static final String RESTORE_DLQ = "grant.award.restore.dlq";

    private Channels() {
    }
}
