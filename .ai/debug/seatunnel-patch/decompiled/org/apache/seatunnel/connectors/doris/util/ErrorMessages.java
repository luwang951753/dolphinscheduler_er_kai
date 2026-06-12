/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.util;

public abstract class ErrorMessages {
    public static final String PARSE_NUMBER_FAILED_MESSAGE = "Parse '{}' to number failed. Original string is '{}'.";
    public static final String PARSE_BOOL_FAILED_MESSAGE = "Parse '{}' to boolean failed. Original string is '{}'.";
    public static final String CONNECT_FAILED_MESSAGE = "Connect to doris {} failed.";
    public static final String ILLEGAL_ARGUMENT_MESSAGE = "argument '{}' is illegal, value is '{}'.";
    public static final String SHOULD_NOT_HAPPEN_MESSAGE = "Should not come here.";
    public static final String DORIS_INTERNAL_FAIL_MESSAGE = "Doris server '{}' internal failed, status is '{}', error message is '{}'";
}

