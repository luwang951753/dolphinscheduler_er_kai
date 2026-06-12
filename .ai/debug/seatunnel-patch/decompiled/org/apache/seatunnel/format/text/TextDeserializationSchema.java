/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.NonNull
 */
package org.apache.seatunnel.format.text;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.table.type.ArrayType;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.MapType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.common.utils.DateTimeUtils;
import org.apache.seatunnel.common.utils.DateUtils;
import org.apache.seatunnel.common.utils.TimeUtils;
import org.apache.seatunnel.format.text.constant.TextFormatConstant;
import org.apache.seatunnel.format.text.exception.SeaTunnelTextFormatException;

public class TextDeserializationSchema
implements DeserializationSchema<SeaTunnelRow> {
    private final SeaTunnelRowType seaTunnelRowType;
    private final String[] separators;
    private final DateUtils.Formatter dateFormatter;
    private final DateTimeUtils.Formatter dateTimeFormatter;
    private final TimeUtils.Formatter timeFormatter;

    private TextDeserializationSchema(@NonNull SeaTunnelRowType seaTunnelRowType, String[] separators, DateUtils.Formatter dateFormatter, DateTimeUtils.Formatter dateTimeFormatter, TimeUtils.Formatter timeFormatter) {
        if (seaTunnelRowType == null) {
            throw new NullPointerException("seaTunnelRowType is marked non-null but is null");
        }
        this.seaTunnelRowType = seaTunnelRowType;
        this.separators = separators;
        this.dateFormatter = dateFormatter;
        this.dateTimeFormatter = dateTimeFormatter;
        this.timeFormatter = timeFormatter;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SeaTunnelRow deserialize(byte[] message) throws IOException {
        String content = new String(message);
        Map<Integer, String> splitsMap = this.splitLineBySeaTunnelRowType(content, this.seaTunnelRowType, 0);
        Object[] objects = new Object[this.seaTunnelRowType.getTotalFields()];
        for (int i = 0; i < objects.length; ++i) {
            objects[i] = this.convert(splitsMap.get(i), this.seaTunnelRowType.getFieldType(i), 0);
        }
        return new SeaTunnelRow(objects);
    }

    @Override
    public SeaTunnelDataType<SeaTunnelRow> getProducedType() {
        return this.seaTunnelRowType;
    }

    private Map<Integer, String> splitLineBySeaTunnelRowType(String line, SeaTunnelRowType seaTunnelRowType, int level) {
        int i;
        String[] splits = line.split(this.separators[level], -1);
        LinkedHashMap<Integer, String> splitsMap = new LinkedHashMap<Integer, String>();
        SeaTunnelDataType<?>[] fieldTypes = seaTunnelRowType.getFieldTypes();
        for (i = 0; i < splits.length; ++i) {
            splitsMap.put(i, splits[i]);
        }
        if (fieldTypes.length > splits.length) {
            for (i = splits.length; i < fieldTypes.length; ++i) {
                splitsMap.put(i, null);
            }
        }
        return splitsMap;
    }

    private Object convert(String field, SeaTunnelDataType<?> fieldType, int level) {
        if (StringUtils.isBlank(field)) {
            return null;
        }
        switch (fieldType.getSqlType()) {
            case ARRAY: {
                BasicType elementType = ((ArrayType)fieldType).getElementType();
                String[] elements = field.split(this.separators[level + 1]);
                ArrayList<Object> objectArrayList = new ArrayList<Object>();
                for (String element : elements) {
                    objectArrayList.add(this.convert(element, elementType, level + 1));
                }
                switch (elementType.getSqlType()) {
                    case STRING: {
                        return objectArrayList.toArray(new String[0]);
                    }
                    case BOOLEAN: {
                        return objectArrayList.toArray(new Boolean[0]);
                    }
                    case TINYINT: {
                        return objectArrayList.toArray(new Byte[0]);
                    }
                    case SMALLINT: {
                        return objectArrayList.toArray(new Short[0]);
                    }
                    case INT: {
                        return objectArrayList.toArray(new Integer[0]);
                    }
                    case BIGINT: {
                        return objectArrayList.toArray(new Long[0]);
                    }
                    case FLOAT: {
                        return objectArrayList.toArray(new Float[0]);
                    }
                    case DOUBLE: {
                        return objectArrayList.toArray(new Double[0]);
                    }
                }
                throw new SeaTunnelTextFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, String.format("SeaTunnel array not support this data type [%s]", new Object[]{elementType.getSqlType()}));
            }
            case MAP: {
                String[] kvs;
                SeaTunnelDataType keyType = ((MapType)fieldType).getKeyType();
                SeaTunnelDataType valueType = ((MapType)fieldType).getValueType();
                LinkedHashMap<Object, Object> objectMap = new LinkedHashMap<Object, Object>();
                for (String kv : kvs = field.split(this.separators[level + 1])) {
                    String[] splits = kv.split(this.separators[level + 2]);
                    objectMap.put(this.convert(splits[0], keyType, level + 1), this.convert(splits[1], valueType, level + 1));
                }
                return objectMap;
            }
            case STRING: {
                return field;
            }
            case BOOLEAN: {
                return Boolean.parseBoolean(field);
            }
            case TINYINT: {
                return Byte.parseByte(field);
            }
            case SMALLINT: {
                return Short.parseShort(field);
            }
            case INT: {
                return Integer.parseInt(field);
            }
            case BIGINT: {
                return Long.parseLong(field);
            }
            case FLOAT: {
                return Float.valueOf(Float.parseFloat(field));
            }
            case DOUBLE: {
                return Double.parseDouble(field);
            }
            case DECIMAL: {
                return new BigDecimal(field);
            }
            case NULL: {
                return null;
            }
            case BYTES: {
                return field.getBytes();
            }
            case DATE: {
                return DateUtils.parse(field, this.dateFormatter);
            }
            case TIME: {
                return TimeUtils.parse(field, this.timeFormatter);
            }
            case TIMESTAMP: {
                return DateTimeUtils.parse(field, this.dateTimeFormatter);
            }
            case ROW: {
                Map<Integer, String> splitsMap = this.splitLineBySeaTunnelRowType(field, (SeaTunnelRowType)fieldType, level + 1);
                Object[] objects = new Object[splitsMap.size()];
                for (int i = 0; i < objects.length; ++i) {
                    objects[i] = this.convert(splitsMap.get(i), ((SeaTunnelRowType)fieldType).getFieldType(i), level + 1);
                }
                return new SeaTunnelRow(objects);
            }
        }
        throw new SeaTunnelTextFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, String.format("SeaTunnel not support this data type [%s]", new Object[]{fieldType.getSqlType()}));
    }

    public static class Builder {
        private SeaTunnelRowType seaTunnelRowType;
        private String[] separators = (String[])TextFormatConstant.SEPARATOR.clone();
        private DateUtils.Formatter dateFormatter = DateUtils.Formatter.YYYY_MM_DD;
        private DateTimeUtils.Formatter dateTimeFormatter = DateTimeUtils.Formatter.YYYY_MM_DD_HH_MM_SS;
        private TimeUtils.Formatter timeFormatter = TimeUtils.Formatter.HH_MM_SS;

        private Builder() {
        }

        public Builder seaTunnelRowType(SeaTunnelRowType seaTunnelRowType) {
            this.seaTunnelRowType = seaTunnelRowType;
            return this;
        }

        public Builder delimiter(String delimiter) {
            this.separators[0] = delimiter;
            return this;
        }

        public Builder separators(String[] separators) {
            this.separators = separators;
            return this;
        }

        public Builder dateFormatter(DateUtils.Formatter dateFormatter) {
            this.dateFormatter = dateFormatter;
            return this;
        }

        public Builder dateTimeFormatter(DateTimeUtils.Formatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
            return this;
        }

        public Builder timeFormatter(TimeUtils.Formatter timeFormatter) {
            this.timeFormatter = timeFormatter;
            return this;
        }

        public TextDeserializationSchema build() {
            return new TextDeserializationSchema(this.seaTunnelRowType, this.separators, this.dateFormatter, this.dateTimeFormatter, this.timeFormatter);
        }
    }
}

