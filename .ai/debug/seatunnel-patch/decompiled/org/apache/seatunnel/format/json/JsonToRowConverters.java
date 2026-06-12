/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.apache.seatunnel.api.table.type.ArrayType;
import org.apache.seatunnel.api.table.type.MapType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.api.table.type.SqlType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.format.json.exception.SeaTunnelJsonFormatException;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.JsonNode;

public class JsonToRowConverters
implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final DateTimeFormatter TIME_FORMAT = new DateTimeFormatterBuilder().appendPattern("HH:mm:ss").appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).toFormatter();
    private final boolean failOnMissingField;
    private final boolean ignoreParseErrors;

    public JsonToRowConverters(boolean failOnMissingField, boolean ignoreParseErrors) {
        this.failOnMissingField = failOnMissingField;
        this.ignoreParseErrors = ignoreParseErrors;
    }

    public JsonToRowConverter createConverter(SeaTunnelDataType<?> type) {
        return this.wrapIntoNullableConverter(this.createNotNullConverter(type));
    }

    private JsonToRowConverter createNotNullConverter(SeaTunnelDataType<?> type) {
        SqlType sqlType = type.getSqlType();
        switch (sqlType) {
            case ROW: {
                return this.createRowConverter((SeaTunnelRowType)type);
            }
            case NULL: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return null;
                    }
                };
            }
            case BOOLEAN: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToBoolean(jsonNode);
                    }
                };
            }
            case TINYINT: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return Byte.parseByte(jsonNode.asText().trim());
                    }
                };
            }
            case SMALLINT: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return Short.parseShort(jsonNode.asText().trim());
                    }
                };
            }
            case INT: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToInt(jsonNode);
                    }
                };
            }
            case BIGINT: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToLong(jsonNode);
                    }
                };
            }
            case DATE: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToLocalDate(jsonNode);
                    }
                };
            }
            case TIME: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToLocalTime(jsonNode);
                    }
                };
            }
            case TIMESTAMP: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToLocalDateTime(jsonNode);
                    }
                };
            }
            case FLOAT: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return Float.valueOf(JsonToRowConverters.this.convertToFloat(jsonNode));
                    }
                };
            }
            case DOUBLE: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToDouble(jsonNode);
                    }
                };
            }
            case STRING: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToString(jsonNode);
                    }
                };
            }
            case BYTES: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToBytes(jsonNode);
                    }
                };
            }
            case DECIMAL: {
                return new JsonToRowConverter(){

                    @Override
                    public Object convert(JsonNode jsonNode) {
                        return JsonToRowConverters.this.convertToBigDecimal(jsonNode);
                    }
                };
            }
            case ARRAY: {
                return this.createArrayConverter((ArrayType)type);
            }
            case MAP: {
                return this.createMapConverter((MapType)type);
            }
        }
        throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, "Unsupported type: " + type);
    }

    private boolean convertToBoolean(JsonNode jsonNode) {
        if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        }
        return Boolean.parseBoolean(jsonNode.asText().trim());
    }

    private int convertToInt(JsonNode jsonNode) {
        if (jsonNode.canConvertToInt()) {
            return jsonNode.asInt();
        }
        return Integer.parseInt(jsonNode.asText().trim());
    }

    private long convertToLong(JsonNode jsonNode) {
        if (jsonNode.canConvertToLong()) {
            return jsonNode.asLong();
        }
        return Long.parseLong(jsonNode.asText().trim());
    }

    private double convertToDouble(JsonNode jsonNode) {
        if (jsonNode.isDouble()) {
            return jsonNode.asDouble();
        }
        return Double.parseDouble(jsonNode.asText().trim());
    }

    private float convertToFloat(JsonNode jsonNode) {
        if (jsonNode.isDouble()) {
            return (float)jsonNode.asDouble();
        }
        return Float.parseFloat(jsonNode.asText().trim());
    }

    private LocalDate convertToLocalDate(JsonNode jsonNode) {
        return DateTimeFormatter.ISO_LOCAL_DATE.parse(jsonNode.asText()).query(TemporalQueries.localDate());
    }

    private LocalTime convertToLocalTime(JsonNode jsonNode) {
        TemporalAccessor parsedTime = TIME_FORMAT.parse(jsonNode.asText());
        return parsedTime.query(TemporalQueries.localTime());
    }

    private LocalDateTime convertToLocalDateTime(JsonNode jsonNode) {
        TemporalAccessor parsedTimestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(jsonNode.asText());
        LocalTime localTime = parsedTimestamp.query(TemporalQueries.localTime());
        LocalDate localDate = parsedTimestamp.query(TemporalQueries.localDate());
        return LocalDateTime.of(localDate, localTime);
    }

    private String convertToString(JsonNode jsonNode) {
        if (jsonNode.isContainerNode()) {
            return jsonNode.toString();
        }
        return jsonNode.asText();
    }

    private byte[] convertToBytes(JsonNode jsonNode) {
        try {
            return jsonNode.binaryValue();
        }
        catch (IOException e) {
            throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, "Unable to deserialize byte array.", e);
        }
    }

    private BigDecimal convertToBigDecimal(JsonNode jsonNode) {
        BigDecimal bigDecimal = jsonNode.isBigDecimal() ? jsonNode.decimalValue() : new BigDecimal(jsonNode.asText());
        return bigDecimal;
    }

    private JsonToRowConverter createRowConverter(SeaTunnelRowType rowType) {
        final JsonToRowConverter[] fieldConverters = (JsonToRowConverter[])Arrays.stream(rowType.getFieldTypes()).map(new Function<SeaTunnelDataType<?>, Object>(){

            @Override
            public Object apply(SeaTunnelDataType<?> seaTunnelDataType) {
                return JsonToRowConverters.this.createConverter(seaTunnelDataType);
            }
        }).toArray((IntFunction<A[]>)new IntFunction<JsonToRowConverter[]>(){

            @Override
            public JsonToRowConverter[] apply(int value) {
                return new JsonToRowConverter[value];
            }
        });
        final String[] fieldNames = rowType.getFieldNames();
        return new JsonToRowConverter(){

            @Override
            public Object convert(JsonNode jsonNode) {
                int arity = fieldNames.length;
                SeaTunnelRow row = new SeaTunnelRow(arity);
                for (int i = 0; i < arity; ++i) {
                    String fieldName = fieldNames[i];
                    JsonNode field = jsonNode.isArray() ? jsonNode.get(i) : jsonNode.get(fieldName);
                    try {
                        Object convertedField = JsonToRowConverters.this.convertField(fieldConverters[i], fieldName, field);
                        row.setField(i, convertedField);
                        continue;
                    }
                    catch (Throwable t) {
                        throw new SeaTunnelJsonFormatException(CommonErrorCode.JSON_OPERATION_FAILED, String.format("Fail to deserialize at field: %s.", fieldName), t);
                    }
                }
                return row;
            }
        };
    }

    private JsonToRowConverter createArrayConverter(final ArrayType<?, ?> type) {
        final JsonToRowConverter valueConverter = this.createConverter(type.getElementType());
        return new JsonToRowConverter(){

            @Override
            public Object convert(JsonNode jsonNode) {
                Object arr = Array.newInstance(type.getElementType().getTypeClass(), jsonNode.size());
                for (int i = 0; i < jsonNode.size(); ++i) {
                    Array.set(arr, i, valueConverter.convert(jsonNode.get(i)));
                }
                return arr;
            }
        };
    }

    private JsonToRowConverter createMapConverter(MapType<?, ?> type) {
        final JsonToRowConverter valueConverter = this.createConverter(type.getValueType());
        return new JsonToRowConverter(){

            @Override
            public Object convert(JsonNode jsonNode) {
                final HashMap value = new HashMap();
                jsonNode.fields().forEachRemaining(new Consumer<Map.Entry<String, JsonNode>>(){

                    @Override
                    public void accept(Map.Entry<String, JsonNode> entry) {
                        value.put(entry.getKey(), valueConverter.convert(entry.getValue()));
                    }
                });
                return value;
            }
        };
    }

    private Object convertField(JsonToRowConverter fieldConverter, String fieldName, JsonNode field) {
        if (field == null) {
            if (this.failOnMissingField) {
                throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.JSON_OPERATION_FAILED, String.format("Could not find field with name %s .", fieldName));
            }
            return null;
        }
        return fieldConverter.convert(field);
    }

    private JsonToRowConverter wrapIntoNullableConverter(final JsonToRowConverter converter) {
        return new JsonToRowConverter(){

            @Override
            public Object convert(JsonNode jsonNode) {
                if (jsonNode == null || jsonNode.isNull() || jsonNode.isMissingNode()) {
                    return null;
                }
                try {
                    return converter.convert(jsonNode);
                }
                catch (Throwable t) {
                    if (!JsonToRowConverters.this.ignoreParseErrors) {
                        throw t;
                    }
                    return null;
                }
            }
        };
    }

    private static final class JsonParseException
    extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JsonParseException(String message) {
            super(message);
        }

        public JsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @FunctionalInterface
    public static interface JsonToRowConverter
    extends Serializable {
        public Object convert(JsonNode var1);
    }
}

