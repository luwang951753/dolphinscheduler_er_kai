/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.debezium;

import java.util.Map;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.format.json.JsonFormatOptions;

public class DebeziumJsonFormatOptions {
    public static final int GENERATE_ROW_SIZE = 3;
    public static final Option<Boolean> IGNORE_PARSE_ERRORS = JsonFormatOptions.IGNORE_PARSE_ERRORS;
    public static final Option<Boolean> SCHEMA_INCLUDE = Options.key("schema-include").booleanType().defaultValue(false).withDescription("When setting up a Debezium Kafka Connect, users can enable a Kafka configuration 'value.converter.schemas.enable' to include schema in the message. This option indicates the Debezium JSON data include the schema in the message or not. Default is false.");

    public static boolean getSchemaInclude(Map<String, String> options) {
        return Boolean.parseBoolean(options.getOrDefault(SCHEMA_INCLUDE.key(), SCHEMA_INCLUDE.defaultValue().toString()));
    }

    public static boolean getIgnoreParseErrors(Map<String, String> options) {
        return Boolean.parseBoolean(options.getOrDefault(IGNORE_PARSE_ERRORS.key(), IGNORE_PARSE_ERRORS.defaultValue().toString()));
    }
}

