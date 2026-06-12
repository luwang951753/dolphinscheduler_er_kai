/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.debezium;

import org.apache.seatunnel.api.serialization.SerializationSchema;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.format.json.JsonSerializationSchema;
import org.apache.seatunnel.format.json.exception.SeaTunnelJsonFormatException;

public class DebeziumJsonSerializationSchema
implements SerializationSchema {
    private static final long serialVersionUID = 1L;
    private static final String OP_INSERT = "c";
    private static final String OP_DELETE = "d";
    private final JsonSerializationSchema jsonSerializer;
    private transient SeaTunnelRow genericRow;

    public DebeziumJsonSerializationSchema(SeaTunnelRowType rowType) {
        this.jsonSerializer = new JsonSerializationSchema(DebeziumJsonSerializationSchema.createJsonRowType(rowType));
        this.genericRow = new SeaTunnelRow(3);
    }

    @Override
    public byte[] serialize(SeaTunnelRow row) {
        try {
            switch (row.getRowKind()) {
                case INSERT: 
                case UPDATE_AFTER: {
                    this.genericRow.setField(0, null);
                    this.genericRow.setField(1, row);
                    this.genericRow.setField(2, OP_INSERT);
                    return this.jsonSerializer.serialize(this.genericRow);
                }
                case UPDATE_BEFORE: 
                case DELETE: {
                    this.genericRow.setField(0, row);
                    this.genericRow.setField(1, null);
                    this.genericRow.setField(2, OP_DELETE);
                    return this.jsonSerializer.serialize(this.genericRow);
                }
            }
            throw new UnsupportedOperationException(String.format("Unsupported operation '%s' for row kind.", new Object[]{row.getRowKind()}));
        }
        catch (Throwable t) {
            throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, String.format("Could not serialize row %s.", row), t);
        }
    }

    private static SeaTunnelRowType createJsonRowType(SeaTunnelRowType databaseSchema) {
        return new SeaTunnelRowType(new String[]{"before", "after", "op"}, new SeaTunnelDataType[]{databaseSchema, databaseSchema, BasicType.STRING_TYPE});
    }
}

