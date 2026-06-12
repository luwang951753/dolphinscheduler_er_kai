/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.NonNull
 */
package org.apache.seatunnel.format.text;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.apache.seatunnel.api.serialization.SerializationSchema;
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

public class TextSerializationSchema
implements SerializationSchema {
    private final SeaTunnelRowType seaTunnelRowType;
    private final String[] separators;
    private final DateUtils.Formatter dateFormatter;
    private final DateTimeUtils.Formatter dateTimeFormatter;
    private final TimeUtils.Formatter timeFormatter;

    private TextSerializationSchema(@NonNull SeaTunnelRowType seaTunnelRowType, String[] separators, DateUtils.Formatter dateFormatter, DateTimeUtils.Formatter dateTimeFormatter, TimeUtils.Formatter timeFormatter) {
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
    public byte[] serialize(SeaTunnelRow element) {
        if (element.getFields().length != this.seaTunnelRowType.getTotalFields()) {
            throw new IndexOutOfBoundsException("The data does not match the configured schema information, please check");
        }
        Object[] fields = element.getFields();
        CharSequence[] strings = new String[fields.length];
        for (int i = 0; i < fields.length; ++i) {
            strings[i] = this.convert(fields[i], this.seaTunnelRowType.getFieldType(i), 0);
        }
        return String.join((CharSequence)this.separators[0], strings).getBytes();
    }

    private String convert(Object field, SeaTunnelDataType<?> fieldType, int level) {
        if (field == null) {
            return "";
        }
        switch (fieldType.getSqlType()) {
            case DOUBLE: 
            case FLOAT: 
            case INT: 
            case STRING: 
            case BOOLEAN: 
            case TINYINT: 
            case SMALLINT: 
            case BIGINT: 
            case DECIMAL: {
                return field.toString();
            }
            case DATE: {
                return DateUtils.toString((LocalDate)field, this.dateFormatter);
            }
            case TIME: {
                return TimeUtils.toString((LocalTime)field, this.timeFormatter);
            }
            case TIMESTAMP: {
                return DateTimeUtils.toString((LocalDateTime)field, this.dateTimeFormatter);
            }
            case NULL: {
                return "";
            }
            case BYTES: {
                return new String((byte[])field);
            }
            case ARRAY: {
                BasicType elementType = ((ArrayType)fieldType).getElementType();
                return Arrays.stream((Object[])field).map(f -> this.convert(f, elementType, level + 1)).collect(Collectors.joining(this.separators[level + 1]));
            }
            case MAP: {
                SeaTunnelDataType keyType = ((MapType)fieldType).getKeyType();
                SeaTunnelDataType valueType = ((MapType)fieldType).getValueType();
                return ((Map)field).entrySet().stream().map(entry -> String.join((CharSequence)this.separators[level + 2], this.convert(entry.getKey(), keyType, level + 1), this.convert(entry.getValue(), valueType, level + 1))).collect(Collectors.joining(this.separators[level + 1]));
            }
            case ROW: {
                Object[] fields = ((SeaTunnelRow)field).getFields();
                CharSequence[] strings = new String[fields.length];
                for (int i = 0; i < fields.length; ++i) {
                    strings[i] = this.convert(fields[i], ((SeaTunnelRowType)fieldType).getFieldType(i), level + 1);
                }
                return String.join((CharSequence)this.separators[level + 1], strings);
            }
        }
        throw new SeaTunnelTextFormatException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, String.format("SeaTunnel format text not supported for parsing this type [%s]", new Object[]{fieldType.getSqlType()}));
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

        public TextSerializationSchema build() {
            return new TextSerializationSchema(this.seaTunnelRowType, this.separators, this.dateFormatter, this.dateTimeFormatter, this.timeFormatter);
        }
    }
}

