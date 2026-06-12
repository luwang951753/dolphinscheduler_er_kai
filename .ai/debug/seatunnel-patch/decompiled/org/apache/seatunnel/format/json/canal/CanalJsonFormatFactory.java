/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.canal;

import java.util.Map;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.serialization.SerializationSchema;
import org.apache.seatunnel.api.table.connector.DeserializationFormat;
import org.apache.seatunnel.api.table.connector.SerializationFormat;
import org.apache.seatunnel.api.table.factory.DeserializationFormatFactory;
import org.apache.seatunnel.api.table.factory.SerializationFormatFactory;
import org.apache.seatunnel.api.table.factory.TableFactoryContext;
import org.apache.seatunnel.format.json.canal.CanalJsonDeserializationSchema;
import org.apache.seatunnel.format.json.canal.CanalJsonFormatOptions;
import org.apache.seatunnel.format.json.canal.CanalJsonSerializationSchema;

public class CanalJsonFormatFactory
implements DeserializationFormatFactory,
SerializationFormatFactory {
    public static final String IDENTIFIER = "canal_json";

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
                return new CanalJsonSerializationSchema(null);
            }
        };
    }

    @Override
    public DeserializationFormat createDeserializationFormat(TableFactoryContext context) {
        Map<String, String> options = context.getOptions().toMap();
        final boolean ignoreParseErrors = CanalJsonFormatOptions.getIgnoreParseErrors(options);
        final String databaseInclude = CanalJsonFormatOptions.getDatabaseInclude(options);
        final String tableInclude = CanalJsonFormatOptions.getTableInclude(options);
        return new DeserializationFormat(){

            @Override
            public DeserializationSchema createDeserializationSchema() {
                return new CanalJsonDeserializationSchema(null, databaseInclude, tableInclude, ignoreParseErrors);
            }
        };
    }
}

