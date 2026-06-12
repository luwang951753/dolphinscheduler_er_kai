/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.debezium;

import java.util.Map;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.serialization.SerializationSchema;
import org.apache.seatunnel.api.table.connector.DeserializationFormat;
import org.apache.seatunnel.api.table.connector.SerializationFormat;
import org.apache.seatunnel.api.table.factory.DeserializationFormatFactory;
import org.apache.seatunnel.api.table.factory.SerializationFormatFactory;
import org.apache.seatunnel.api.table.factory.TableFactoryContext;
import org.apache.seatunnel.format.json.debezium.DebeziumJsonDeserializationSchema;
import org.apache.seatunnel.format.json.debezium.DebeziumJsonFormatOptions;
import org.apache.seatunnel.format.json.debezium.DebeziumJsonSerializationSchema;

public class DebeziumJsonFormatFactory
implements DeserializationFormatFactory,
SerializationFormatFactory {
    public static final String IDENTIFIER = "debezium_json";

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder().build();
    }

    @Override
    public SerializationFormat createSerializationFormat(TableFactoryContext context) {
        return new SerializationFormat(){

            @Override
            public SerializationSchema createSerializationSchema() {
                return new DebeziumJsonSerializationSchema(null);
            }
        };
    }

    @Override
    public DeserializationFormat createDeserializationFormat(TableFactoryContext context) {
        Map<String, String> options = context.getOptions().toMap();
        final boolean ignoreParseErrors = DebeziumJsonFormatOptions.getIgnoreParseErrors(options);
        boolean schemaInclude = DebeziumJsonFormatOptions.getSchemaInclude(options);
        return new DeserializationFormat(){

            @Override
            public DeserializationSchema createDeserializationSchema() {
                return new DebeziumJsonDeserializationSchema(null, ignoreParseErrors);
            }
        };
    }
}

