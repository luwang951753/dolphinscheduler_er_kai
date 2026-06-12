/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.canal;

import org.apache.seatunnel.api.serialization.SerializationSchema;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.RowKind;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.format.json.JsonSerializationSchema;
import org.apache.seatunnel.format.json.exception.SeaTunnelJsonFormatException;

public class CanalJsonSerializationSchema
implements SerializationSchema {
    private static final long serialVersionUID = 1L;
    private static final String OP_INSERT = "INSERT";
    private static final String OP_DELETE = "DELETE";
    private transient SeaTunnelRow reuse;
    private final JsonSerializationSchema jsonSerializer;

    public CanalJsonSerializationSchema(SeaTunnelRowType rowType) {
        this.jsonSerializer = new JsonSerializationSchema(CanalJsonSerializationSchema.createJsonRowType(rowType));
        this.reuse = new SeaTunnelRow(2);
    }

    @Override
    public byte[] serialize(SeaTunnelRow row) {
        try {
            String opType = this.rowKind2String(row.getRowKind());
            this.reuse.setField(0, row);
            this.reuse.setField(1, opType);
            return this.jsonSerializer.serialize(this.reuse);
        }
        catch (Throwable t) {
            throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, String.format("Could not serialize row %s.", row), t);
        }
    }

    private String rowKind2String(RowKind rowKind) {
        switch (rowKind) {
            case INSERT: 
            case UPDATE_AFTER: {
                return OP_INSERT;
            }
            case UPDATE_BEFORE: 
            case DELETE: {
                return OP_DELETE;
            }
        }
        throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_OPERATION, String.format("Unsupported operation %s for row kind.", new Object[]{rowKind}));
    }

    private static SeaTunnelRowType createJsonRowType(SeaTunnelRowType databaseSchema) {
        return new SeaTunnelRowType(new String[]{"data", "type"}, new SeaTunnelDataType[]{databaseSchema, BasicType.STRING_TYPE});
    }
}

