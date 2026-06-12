/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class TimeFormat {
    private static final int MAX_TIME_PRECISION = 9;
    public static final DateTimeFormatter TIME_FORMAT = new DateTimeFormatterBuilder().appendPattern("HH:mm:ss").appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).toFormatter();
}

