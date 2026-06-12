/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.debezium;

import java.io.IOException;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.table.type.RowKind;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.format.json.JsonDeserializationSchema;
import org.apache.seatunnel.format.json.exception.SeaTunnelJsonFormatException;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.JsonNode;

public class DebeziumJsonDeserializationSchema
implements DeserializationSchema<SeaTunnelRow> {
    private static final long serialVersionUID = 1L;
    private static final String OP_READ = "r";
    private static final String OP_CREATE = "c";
    private static final String OP_UPDATE = "u";
    private static final String OP_DELETE = "d";
    private static final String REPLICA_IDENTITY_EXCEPTION = "The \"before\" field of %s message is null, if you are using Debezium Postgres Connector, please check the Postgres table has been set REPLICA IDENTITY to FULL level.";
    private final SeaTunnelRowType rowType;
    private final JsonDeserializationSchema jsonDeserializer;
    private final boolean ignoreParseErrors;
    private final boolean debeziumEnabledSchema;

    public DebeziumJsonDeserializationSchema(SeaTunnelRowType rowType, boolean ignoreParseErrors) {
        this.rowType = rowType;
        this.ignoreParseErrors = ignoreParseErrors;
        this.jsonDeserializer = new JsonDeserializationSchema(false, ignoreParseErrors, DebeziumJsonDeserializationSchema.createJsonRowType(rowType));
        this.debeziumEnabledSchema = false;
    }

    public DebeziumJsonDeserializationSchema(SeaTunnelRowType rowType, boolean ignoreParseErrors, boolean debeziumEnabledSchema) {
        this.rowType = rowType;
        this.ignoreParseErrors = ignoreParseErrors;
        this.jsonDeserializer = new JsonDeserializationSchema(false, ignoreParseErrors, DebeziumJsonDeserializationSchema.createJsonRowType(rowType));
        this.debeziumEnabledSchema = debeziumEnabledSchema;
    }

    @Override
    public SeaTunnelRow deserialize(byte[] message) throws IOException {
        throw new UnsupportedOperationException("Please invoke DeserializationSchema#deserialize(byte[], Collector<SeaTunnelRow>) instead.");
    }

    @Override
    public void deserialize(byte[] message, Collector<SeaTunnelRow> out) throws IOException {
        block12: {
            if (message == null || message.length == 0) {
                return;
            }
            try {
                JsonNode payload = this.getPayload(this.convertBytes(message));
                String op = payload.get("op").asText();
                if (OP_CREATE.equals(op) || OP_READ.equals(op)) {
                    SeaTunnelRow insert = this.convertJsonNode(payload.get("after"));
                    insert.setRowKind(RowKind.INSERT);
                    out.collect(insert);
                } else if (OP_UPDATE.equals(op)) {
                    SeaTunnelRow before = this.convertJsonNode(payload.get("before"));
                    if (before == null) {
                        throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, String.format(REPLICA_IDENTITY_EXCEPTION, "UPDATE"));
                    }
                    before.setRowKind(RowKind.UPDATE_BEFORE);
                    out.collect(before);
                    SeaTunnelRow after = this.convertJsonNode(payload.get("after"));
                    after.setRowKind(RowKind.UPDATE_AFTER);
                    out.collect(after);
                } else if (OP_DELETE.equals(op)) {
                    SeaTunnelRow delete = this.convertJsonNode(payload.get("before"));
                    if (delete == null) {
                        throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, String.format(REPLICA_IDENTITY_EXCEPTION, "UPDATE"));
                    }
                    delete.setRowKind(RowKind.DELETE);
                    out.collect(delete);
                } else if (!this.ignoreParseErrors) {
                    throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, String.format("Unknown \"op\" value \"%s\". The Debezium JSON message is '%s'", op, new String(message)));
                }
            }
            catch (Throwable t) {
                if (this.ignoreParseErrors) break block12;
                throw new SeaTunnelJsonFormatException(CommonErrorCode.UNSUPPORTED_DATA_TYPE, String.format("Corrupt Debezium JSON message '%s'.", new String(message)), t);
            }
        }
    }

    private JsonNode getPayload(JsonNode jsonNode) {
        if (this.debeziumEnabledSchema) {
            return jsonNode.get("payload");
        }
        return jsonNode;
    }

    private JsonNode convertBytes(byte[] message) {
        try {
            return this.jsonDeserializer.deserializeToJsonNode(message);
        }
        catch (Exception t) {
            if (this.ignoreParseErrors) {
                return null;
            }
            throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, String.format("Failed to deserialize JSON '%s'.", new String(message)), t);
        }
    }

    private SeaTunnelRow convertJsonNode(JsonNode root) {
        return this.jsonDeserializer.convertToRowData(root);
    }

    @Override
    public SeaTunnelDataType<SeaTunnelRow> getProducedType() {
        return this.rowType;
    }

    private static SeaTunnelRowType createJsonRowType(SeaTunnelRowType databaseSchema) {
        return databaseSchema;
    }
}

