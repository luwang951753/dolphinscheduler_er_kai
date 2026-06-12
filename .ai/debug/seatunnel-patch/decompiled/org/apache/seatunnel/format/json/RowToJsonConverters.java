/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
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
import org.apache.seatunnel.format.json.TimeFormat;
import org.apache.seatunnel.format.json.exception.SeaTunnelJsonFormatException;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.JsonNode;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;

public class RowToJsonConverters
implements Serializable {
    private static final long serialVersionUID = 6988876688930916940L;

    public RowToJsonConverter createConverter(SeaTunnelDataType<?> type) {
        return this.wrapIntoNullableConverter(this.createNotNullConverter(type));
    }

    private RowToJsonConverter wrapIntoNullableConverter(final RowToJsonConverter converter) {
        return new RowToJsonConverter(){

            @Override
            public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                if (value == null) {
                    return mapper.getNodeFactory().nullNode();
                }
                return converter.convert(mapper, reuse, value);
            }
        };
    }

    private RowToJsonConverter createNotNullConverter(SeaTunnelDataType<?> type) {
        SqlType sqlType = type.getSqlType();
        switch (sqlType) {
            case ROW: {
                return this.createRowConverter((SeaTunnelRowType)type);
            }
            case NULL: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return null;
                    }
                };
            }
            case BOOLEAN: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().booleanNode((Boolean)value);
                    }
                };
            }
            case TINYINT: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().numberNode((byte)((Byte)value));
                    }
                };
            }
            case SMALLINT: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().numberNode((short)((Short)value));
                    }
                };
            }
            case INT: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().numberNode((int)((Integer)value));
                    }
                };
            }
            case BIGINT: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().numberNode((long)((Long)value));
                    }
                };
            }
            case FLOAT: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().numberNode(((Float)value).floatValue());
                    }
                };
            }
            case DOUBLE: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().numberNode((double)((Double)value));
                    }
                };
            }
            case DECIMAL: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().numberNode((BigDecimal)value);
                    }
                };
            }
            case BYTES: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().binaryNode((byte[])value);
                    }
                };
            }
            case STRING: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().textNode((String)value);
                    }
                };
            }
            case DATE: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().textNode(DateTimeFormatter.ISO_LOCAL_DATE.format((LocalDate)value));
                    }
                };
            }
            case TIME: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().textNode(TimeFormat.TIME_FORMAT.format((LocalTime)value));
                    }
                };
            }
            case TIMESTAMP: {
                return new RowToJsonConverter(){

                    @Override
                    public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                        return mapper.getNodeFactory().textNode(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format((LocalDateTime)value));
                    }
                };
            }
            case ARRAY: {
                return this.createArrayConverter((ArrayType)type);
            }
            case MAP: {
                MapType mapType = (MapType)type;
                return this.createMapConverter(mapType.toString(), mapType.getKeyType(), mapType.getValueType());
            }
        }
        throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, "unsupported parse type: " + type);
    }

    private RowToJsonConverter createRowConverter(SeaTunnelRowType rowType) {
        final RowToJsonConverter[] fieldConverters = (RowToJsonConverter[])Arrays.stream(rowType.getFieldTypes()).map(new Function<SeaTunnelDataType<?>, Object>(){

            @Override
            public Object apply(SeaTunnelDataType<?> seaTunnelDataType) {
                return RowToJsonConverters.this.createConverter(seaTunnelDataType);
            }
        }).toArray((IntFunction<A[]>)new IntFunction<RowToJsonConverter[]>(){

            @Override
            public RowToJsonConverter[] apply(int value) {
                return new RowToJsonConverter[value];
            }
        });
        final String[] fieldNames = rowType.getFieldNames();
        final int arity = fieldNames.length;
        return new RowToJsonConverter(){

            @Override
            public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                ObjectNode node = reuse == null || reuse.isNull() ? mapper.createObjectNode() : (ObjectNode)reuse;
                for (int i = 0; i < arity; ++i) {
                    String fieldName = fieldNames[i];
                    SeaTunnelRow row = (SeaTunnelRow)value;
                    node.set(fieldName, fieldConverters[i].convert(mapper, node.get(fieldName), row.getField(i)));
                }
                return node;
            }
        };
    }

    private RowToJsonConverter createArrayConverter(ArrayType arrayType) {
        final RowToJsonConverter elementConverter = this.createConverter(arrayType.getElementType());
        return new RowToJsonConverter(){

            @Override
            public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                ArrayNode node;
                if (reuse == null || reuse.isNull()) {
                    node = mapper.createArrayNode();
                } else {
                    node = (ArrayNode)reuse;
                    node.removeAll();
                }
                for (Object element : (Object[])value) {
                    node.add(elementConverter.convert(mapper, null, element));
                }
                return node;
            }
        };
    }

    private RowToJsonConverter createMapConverter(String typeSummary, SeaTunnelDataType<?> keyType, SeaTunnelDataType<?> valueType) {
        if (!SqlType.STRING.equals((Object)keyType.getSqlType())) {
            throw new SeaTunnelJsonFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, "JSON format doesn't support non-string as key type of map. The type is: " + typeSummary);
        }
        final RowToJsonConverter valueConverter = this.createConverter(valueType);
        return new RowToJsonConverter(){

            @Override
            public JsonNode convert(ObjectMapper mapper, JsonNode reuse, Object value) {
                ObjectNode node;
                if (reuse == null || reuse.isNull()) {
                    node = mapper.createObjectNode();
                } else {
                    node = (ObjectNode)reuse;
                    node.removeAll();
                }
                Map mapData = (Map)value;
                for (Map.Entry entry : mapData.entrySet()) {
                    String fieldName = (String)entry.getKey();
                    node.set(fieldName, valueConverter.convert(mapper, node.get(fieldName), entry.getValue()));
                }
                return node;
            }
        };
    }

    public static interface RowToJsonConverter
    extends Serializable {
        public JsonNode convert(ObjectMapper var1, JsonNode var2, Object var3);
    }
}

