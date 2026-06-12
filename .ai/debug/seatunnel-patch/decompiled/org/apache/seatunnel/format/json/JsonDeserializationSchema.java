/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json;

import com.google.common.base.Preconditions;
import java.io.IOException;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.table.type.CompositeType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.api.table.type.SqlType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.format.json.JsonToRowConverters;
import org.apache.seatunnel.format.json.exception.SeaTunnelJsonFormatException;
import org.apache.seatunnel.shade.com.fasterxml.jackson.core.json.JsonReadFeature;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.JsonNode;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.NullNode;

public class JsonDeserializationSchema
implements DeserializationSchema<SeaTunnelRow> {
    private static final long serialVersionUID = 1L;
    private final boolean failOnMissingField;
    private final boolean ignoreParseErrors;
    private final SeaTunnelRowType rowType;
    private final JsonToRowConverters.JsonToRowConverter runtimeConverter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonDeserializationSchema(boolean failOnMissingField, boolean ignoreParseErrors, SeaTunnelRowType rowType) {
        if (ignoreParseErrors && failOnMissingField) {
            throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.ILLEGAL_ARGUMENT, "JSON format doesn't support failOnMissingField and ignoreParseErrors are both enabled.");
        }
        this.rowType = Preconditions.checkNotNull(rowType);
        this.failOnMissingField = failOnMissingField;
        this.ignoreParseErrors = ignoreParseErrors;
        this.runtimeConverter = new JsonToRowConverters(failOnMissingField, ignoreParseErrors).createConverter(Preconditions.checkNotNull(rowType));
        if (JsonDeserializationSchema.hasDecimalType(rowType)) {
            this.objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        }
        this.objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
    }

    private static boolean hasDecimalType(SeaTunnelDataType<?> dataType) {
        if (dataType.getSqlType() == SqlType.DECIMAL) {
            return true;
        }
        if (dataType instanceof CompositeType) {
            CompositeType compositeType = (CompositeType)dataType;
            for (SeaTunnelDataType<?> child : compositeType.getChildren()) {
                if (!JsonDeserializationSchema.hasDecimalType(child)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public SeaTunnelRow deserialize(byte[] message) throws IOException {
        if (message == null) {
            return null;
        }
        return this.convertJsonNode(this.convertBytes(message));
    }

    public SeaTunnelRow deserialize(String message) throws IOException {
        if (message == null) {
            return null;
        }
        return this.convertJsonNode(this.convert(message));
    }

    public void collect(byte[] message, Collector<SeaTunnelRow> out) throws IOException {
        JsonNode jsonNode = this.convertBytes(message);
        if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode)jsonNode;
            for (int i = 0; i < arrayNode.size(); ++i) {
                SeaTunnelRow deserialize = this.convertJsonNode(arrayNode.get(i));
                out.collect(deserialize);
            }
        } else {
            SeaTunnelRow deserialize = this.convertJsonNode(jsonNode);
            out.collect(deserialize);
        }
    }

    private SeaTunnelRow convertJsonNode(JsonNode jsonNode) {
        if (jsonNode.isNull()) {
            return null;
        }
        try {
            return (SeaTunnelRow)this.runtimeConverter.convert(jsonNode);
        }
        catch (Throwable t) {
            if (this.ignoreParseErrors) {
                return null;
            }
            throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, String.format("Failed to deserialize JSON '%s'.", jsonNode), t);
        }
    }

    public JsonNode deserializeToJsonNode(byte[] message) throws IOException {
        return this.objectMapper.readTree(message);
    }

    public SeaTunnelRow convertToRowData(JsonNode message) {
        return (SeaTunnelRow)this.runtimeConverter.convert(message);
    }

    private JsonNode convertBytes(byte[] message) {
        try {
            return this.objectMapper.readTree(message);
        }
        catch (Throwable t) {
            if (this.ignoreParseErrors) {
                return NullNode.getInstance();
            }
            throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, String.format("Failed to deserialize JSON '%s'.", new String(message)), t);
        }
    }

    private JsonNode convert(String message) {
        try {
            return this.objectMapper.readTree(message);
        }
        catch (Throwable t) {
            if (this.ignoreParseErrors) {
                return NullNode.getInstance();
            }
            throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, String.format("Failed to deserialize JSON '%s'.", message), t);
        }
    }

    public SeaTunnelRowType getProducedType() {
        return this.rowType;
    }
}

