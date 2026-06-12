/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.StringJoiner;
import org.apache.seatunnel.api.table.type.RowKind;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.connectors.doris.serialize.DorisSerializer;
import org.apache.seatunnel.connectors.doris.serialize.SeaTunnelRowConverter;

public class SeaTunnelRowSerializer
extends SeaTunnelRowConverter
implements DorisSerializer {
    String type;
    private ObjectMapper objectMapper;
    private final SeaTunnelRowType seaTunnelRowType;
    private final String fieldDelimiter;
    private final boolean enableDelete;

    public SeaTunnelRowSerializer(String type, SeaTunnelRowType seaTunnelRowType, String fieldDelimiter, boolean enableDelete) {
        this.type = type;
        this.seaTunnelRowType = seaTunnelRowType;
        this.fieldDelimiter = fieldDelimiter;
        this.enableDelete = enableDelete;
        if ("json".equals(type)) {
            this.objectMapper = new ObjectMapper();
        }
    }

    @Override
    public byte[] serialize(SeaTunnelRow seaTunnelRow) throws IOException {
        String valString;
        if ("json".equals(this.type)) {
            valString = this.buildJsonString(seaTunnelRow);
        } else if ("csv".equals(this.type)) {
            valString = this.buildCSVString(seaTunnelRow);
        } else {
            throw new IllegalArgumentException("The type " + this.type + " is not supported!");
        }
        return valString.getBytes(StandardCharsets.UTF_8);
    }

    public String buildJsonString(SeaTunnelRow row) throws IOException {
        HashMap<String, Object> rowMap = new HashMap<String, Object>(row.getFields().length);
        for (int i = 0; i < row.getFields().length; ++i) {
            Object value = this.convert(this.seaTunnelRowType.getFieldType(i), row.getField(i));
            rowMap.put(this.seaTunnelRowType.getFieldName(i), value);
        }
        if (this.enableDelete) {
            rowMap.put("__DORIS_DELETE_SIGN__", this.parseDeleteSign(row.getRowKind()));
        }
        return this.objectMapper.writeValueAsString(rowMap);
    }

    public String buildCSVString(SeaTunnelRow row) throws IOException {
        StringJoiner joiner = new StringJoiner(this.fieldDelimiter);
        for (int i = 0; i < row.getFields().length; ++i) {
            Object field = this.convert(this.seaTunnelRowType.getFieldType(i), row.getField(i));
            String value = field != null ? field.toString() : "\\N";
            joiner.add(value);
        }
        if (this.enableDelete) {
            joiner.add(this.parseDeleteSign(row.getRowKind()));
        }
        return joiner.toString();
    }

    public String parseDeleteSign(RowKind rowKind) {
        if (RowKind.INSERT.equals((Object)rowKind) || RowKind.UPDATE_AFTER.equals((Object)rowKind)) {
            return "0";
        }
        if (RowKind.DELETE.equals((Object)rowKind) || RowKind.UPDATE_BEFORE.equals((Object)rowKind)) {
            return "1";
        }
        throw new IllegalArgumentException("Unrecognized row kind:" + rowKind.toString());
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    public static class Builder {
        private SeaTunnelRowType seaTunnelRowType;
        private String type;
        private String fieldDelimiter;
        private boolean deletable;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setSeaTunnelRowType(SeaTunnelRowType seaTunnelRowType) {
            this.seaTunnelRowType = seaTunnelRowType;
            return this;
        }

        public Builder setFieldDelimiter(String fieldDelimiter) {
            this.fieldDelimiter = fieldDelimiter;
            return this;
        }

        public Builder enableDelete(boolean deletable) {
            this.deletable = deletable;
            return this;
        }

        public SeaTunnelRowSerializer build() {
            Preconditions.checkState("csv".equals(this.type) && this.fieldDelimiter != null || "json".equals(this.type));
            return new SeaTunnelRowSerializer(this.type, this.seaTunnelRowType, this.fieldDelimiter, this.deletable);
        }
    }
}

