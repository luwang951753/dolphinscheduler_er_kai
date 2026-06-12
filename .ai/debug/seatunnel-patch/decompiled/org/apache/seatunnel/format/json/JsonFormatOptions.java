/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json;

import java.util.Map;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;

public class JsonFormatOptions {
    public static final Option<Boolean> FAIL_ON_MISSING_FIELD = Options.key("fail-on-missing-field").booleanType().defaultValue(false).withDescription("Optional flag to specify whether to fail if a field is missing or not, false by default.");
    public static final Option<Boolean> IGNORE_PARSE_ERRORS = Options.key("ignore-parse-errors").booleanType().defaultValue(false).withDescription("Optional flag to skip fields and rows with parse errors instead of failing;\nfields are set to null in case of errors, false by default.");

    public static boolean getFailOnMissingField(Map<String, String> options) {
        return Boolean.parseBoolean(options.getOrDefault(FAIL_ON_MISSING_FIELD.key(), FAIL_ON_MISSING_FIELD.toString()));
    }

    public static boolean getIgnoreParseErrors(Map<String, String> options) {
        return Boolean.parseBoolean(options.getOrDefault(IGNORE_PARSE_ERRORS.key(), IGNORE_PARSE_ERRORS.toString()));
    }
}

