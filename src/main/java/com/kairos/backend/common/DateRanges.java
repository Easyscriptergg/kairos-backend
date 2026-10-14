package com.kairos.backend.common;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Records are keyed by the athlete's local calendar day, which the client
 * sends as YYYY-MM-DD and is stored as a DATE with no time-zone conversion.
 * The server only rejects days that cannot be "today" anywhere on Earth.
 */
public final class DateRanges {

    public static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);

    private DateRanges() {
    }

    /** The latest calendar day that is already "today" in some time zone (UTC+14). */
    public static LocalDate latestPossibleToday() {
        return LocalDate.now(ZoneOffset.ofHours(14));
    }

    public static void requirePastOrToday(LocalDate date) {
        if (date.isAfter(latestPossibleToday())) {
            throw ApiException.badRequest("The date " + date + " is in the future.");
        }
        if (date.isBefore(EARLIEST)) {
            throw ApiException.badRequest("The date " + date + " is too far in the past.");
        }
    }

    public static LocalDate fromOrEarliest(LocalDate from) {
        return from == null ? EARLIEST : from;
    }

    public static LocalDate toOrLatest(LocalDate to) {
        return to == null ? latestPossibleToday() : to;
    }

    public static void requireOrdered(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw ApiException.badRequest("“from” must not be after “to”.");
    }
}
