/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json;

import java.util.Map;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.serialization.SerializationSchema;
import org.apache.seatunnel.api.table.connector.DeserializationFormat;
import org.apache.seatunnel.api.table.connector.SerializationFormat;
import org.apache.seatunnel.api.table.factory.DeserializationFormatFactory;
import org.apache.seatunnel.api.table.factory.SerializationFormatFactory;
import org.apache.seatunnel.api.table.factory.TableFactoryContext;
import org.apache.seatunnel.format.json.JsonDeserializationSchema;
import org.apache.seatunnel.format.json.JsonFormatOptions;
import org.apache.seatunnel.format.json.JsonSerializationSchema;

public class JsonFormatFactory
implements DeserializationFormatFactory,
SerializationFormatFactory {
    public static final String IDENTIFIER = "json";

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder().build();
    }

    @Override
    public DeserializationFormat createDeserializationFormat(TableFactoryContext context) {
        Map<String, String> options = context.getOptions().toMap();
        final boolean failOnMissingField = JsonFormatOptions.getFailOnMissingField(options);
        final boolean ignoreParseErrors = JsonFormatOptions.getIgnoreParseErrors(options);
        return new DeserializationFormat(){

            @Override
            public DeserializationSchema createDeserializationSchema() {
                return new JsonDeserializationSchema(failOnMissingField, ignoreParseErrors, null);
            }
        };
    }

    @Override
    public SerializationFormat createSerializationFormat(TableFactoryContext context) {
        return new SerializationFormat(){

            @Override
            public SerializationSchema createSerializationSchema() {
                return new JsonSerializationSchema(null);
            }
        };
    }
}

