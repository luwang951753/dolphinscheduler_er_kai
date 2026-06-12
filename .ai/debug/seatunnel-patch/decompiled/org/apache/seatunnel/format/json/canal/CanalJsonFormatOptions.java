/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.canal;

import java.util.Map;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.format.json.JsonFormatOptions;

public class CanalJsonFormatOptions {
    public static final Option<Boolean> IGNORE_PARSE_ERRORS = JsonFormatOptions.IGNORE_PARSE_ERRORS;
    public static final Option<String> DATABASE_INCLUDE = Options.key("database.include").stringType().noDefaultValue().withDescription("An optional regular expression to only read the specific databases changelog rows by regular matching the \"database\" meta field in the Canal record.The pattern string is compatible with Java's Pattern.");
    public static final Option<String> TABLE_INCLUDE = Options.key("table.include").stringType().noDefaultValue().withDescription("An optional regular expression to only read the specific tables changelog rows by regular matching the \"table\" meta field in the Canal record.The pattern string is compatible with Java's Pattern.");

    public static String getTableInclude(Map<String, String> options) {
        return options.getOrDefault(TABLE_INCLUDE.key(), null);
    }

    public static String getDatabaseInclude(Map<String, String> options) {
        return options.getOrDefault(DATABASE_INCLUDE.key(), null);
    }

    public static boolean getIgnoreParseErrors(Map<String, String> options) {
        return Boolean.parseBoolean(options.getOrDefault(IGNORE_PARSE_ERRORS.key(), IGNORE_PARSE_ERRORS.toString()));
    }
}

