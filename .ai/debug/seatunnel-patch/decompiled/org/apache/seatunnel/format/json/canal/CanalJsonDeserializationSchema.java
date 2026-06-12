/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.canal;

import java.io.IOException;
import java.util.regex.Pattern;
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
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;

public class CanalJsonDeserializationSchema
implements DeserializationSchema<SeaTunnelRow> {
    private static final long serialVersionUID = 1L;
    private static final String FIELD_OLD = "old";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DATABASE = "database";
    private static final String FIELD_TABLE = "table";
    private static final String OP_INSERT = "INSERT";
    private static final String OP_UPDATE = "UPDATE";
    private static final String OP_DELETE = "DELETE";
    private static final String OP_CREATE = "CREATE";
    private static final String OP_QUERY = "QUERY";
    private static final String OP_ALTER = "ALTER";
    private String database;
    private String table;
    private final String[] fieldNames;
    private final int fieldCount;
    private boolean ignoreParseErrors;
    private final Pattern databasePattern;
    private final Pattern tablePattern;
    private final JsonDeserializationSchema jsonDeserializer;
    private final SeaTunnelRowType physicalRowType;

    public CanalJsonDeserializationSchema(SeaTunnelRowType physicalRowType, String database, String table, boolean ignoreParseErrors) {
        this.physicalRowType = physicalRowType;
        SeaTunnelRowType jsonRowType = CanalJsonDeserializationSchema.createJsonRowType(physicalRowType);
        this.jsonDeserializer = new JsonDeserializationSchema(false, ignoreParseErrors, jsonRowType);
        this.database = database;
        this.table = table;
        this.fieldNames = physicalRowType.getFieldNames();
        this.fieldCount = physicalRowType.getTotalFields();
        this.ignoreParseErrors = ignoreParseErrors;
        this.databasePattern = database == null ? null : Pattern.compile(database);
        this.tablePattern = table == null ? null : Pattern.compile(table);
    }

    @Override
    public SeaTunnelRow deserialize(byte[] message) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeaTunnelDataType<SeaTunnelRow> getProducedType() {
        return this.physicalRowType;
    }

    @Override
    public void deserialize(byte[] message, Collector<SeaTunnelRow> out) {
        if (message == null) {
            return;
        }
        ObjectNode jsonNode = (ObjectNode)this.convertBytes(message);
        assert (jsonNode != null);
        this.deserialize(jsonNode, out);
    }

    public void deserialize(ObjectNode jsonNode, Collector<SeaTunnelRow> out) {
        if (this.database != null && !this.databasePattern.matcher(jsonNode.get(FIELD_DATABASE).asText()).matches()) {
            return;
        }
        if (this.table != null && !this.tablePattern.matcher(jsonNode.get(FIELD_TABLE).asText()).matches()) {
            return;
        }
        JsonNode dataNode = jsonNode.get(FIELD_DATA);
        String type = jsonNode.get(FIELD_TYPE).asText();
        if (dataNode == null || dataNode.isNull()) {
            if (OP_QUERY.equals(type) || OP_CREATE.equals(type) || OP_ALTER.equals(type)) {
                return;
            }
            throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.JSON_OPERATION_FAILED, String.format("Null data value \"%s\" Cannot send downstream", jsonNode));
        }
        if (OP_INSERT.equals(type)) {
            for (int i = 0; i < dataNode.size(); ++i) {
                SeaTunnelRow row = this.convertJsonNode(dataNode.get(i));
                out.collect(row);
            }
        } else if (OP_UPDATE.equals(type)) {
            ArrayNode oldNode = (ArrayNode)jsonNode.get(FIELD_OLD);
            for (int i = 0; i < dataNode.size(); ++i) {
                SeaTunnelRow after = this.convertJsonNode(dataNode.get(i));
                SeaTunnelRow before = this.convertJsonNode(oldNode.get(i));
                for (int f = 0; f < this.fieldCount; ++f) {
                    assert (before != null);
                    if (!before.isNullAt(f) || oldNode.findValue(this.fieldNames[f]) != null) continue;
                    assert (after != null);
                    before.setField(f, after.getField(f));
                }
                assert (before != null);
                before.setRowKind(RowKind.UPDATE_BEFORE);
                assert (after != null);
                after.setRowKind(RowKind.UPDATE_AFTER);
                out.collect(before);
                out.collect(after);
            }
        } else if (OP_DELETE.equals(type)) {
            for (int i = 0; i < dataNode.size(); ++i) {
                SeaTunnelRow row = this.convertJsonNode(dataNode.get(i));
                assert (row != null);
                row.setRowKind(RowKind.DELETE);
                out.collect(row);
            }
        } else if (!this.ignoreParseErrors) {
            throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, String.format("Unknown \"type\" value \"%s\". The Canal JSON message is '%s'", type, jsonNode.asText()));
        }
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

    private static SeaTunnelRowType createJsonRowType(SeaTunnelRowType physicalDataType) {
        return physicalDataType;
    }

    public static Builder builder(SeaTunnelRowType physicalDataType) {
        return new Builder(physicalDataType);
    }

    public static class Builder {
        private boolean ignoreParseErrors = false;
        private String database = null;
        private String table = null;
        private final SeaTunnelRowType physicalDataType;

        public Builder(SeaTunnelRowType physicalDataType) {
            this.physicalDataType = physicalDataType;
        }

        public Builder setDatabase(String database) {
            this.database = database;
            return this;
        }

        public Builder setTable(String table) {
            this.table = table;
            return this;
        }

        public Builder setIgnoreParseErrors(boolean ignoreParseErrors) {
            this.ignoreParseErrors = ignoreParseErrors;
            return this;
        }

        public CanalJsonDeserializationSchema build() {
            return new CanalJsonDeserializationSchema(this.physicalDataType, this.database, this.table, this.ignoreParseErrors);
        }
    }
}

