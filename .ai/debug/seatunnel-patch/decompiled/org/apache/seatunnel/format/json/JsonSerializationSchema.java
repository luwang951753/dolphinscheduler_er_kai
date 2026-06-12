/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json;

import com.google.common.base.Preconditions;
import org.apache.seatunnel.api.serialization.SerializationSchema;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.format.json.RowToJsonConverters;
import org.apache.seatunnel.format.json.exception.SeaTunnelJsonFormatException;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonSerializationSchema
implements SerializationSchema {
    private final SeaTunnelRowType rowType;
    private transient ObjectNode node;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RowToJsonConverters.RowToJsonConverter runtimeConverter;

    public JsonSerializationSchema(SeaTunnelRowType rowType) {
        this.rowType = rowType;
        this.runtimeConverter = new RowToJsonConverters().createConverter(Preconditions.checkNotNull(rowType));
    }

    @Override
    public byte[] serialize(SeaTunnelRow row) {
        if (this.node == null) {
            this.node = this.mapper.createObjectNode();
        }
        try {
            this.runtimeConverter.convert(this.mapper, this.node, row);
            return this.mapper.writeValueAsBytes(this.node);
        }
        catch (Throwable e) {
            throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, String.format("Failed to deserialize JSON '%s'.", row), e);
        }
    }

    public ObjectMapper getMapper() {
        return this.mapper;
    }
}

