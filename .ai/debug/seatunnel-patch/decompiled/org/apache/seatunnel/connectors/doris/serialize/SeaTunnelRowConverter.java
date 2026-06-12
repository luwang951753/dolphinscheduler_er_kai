/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.serialize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.common.utils.DateTimeUtils;
import org.apache.seatunnel.common.utils.DateUtils;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.common.utils.TimeUtils;
import org.apache.seatunnel.connectors.doris.exception.DorisConnectorException;

public class SeaTunnelRowConverter {
    private DateUtils.Formatter dateFormatter = DateUtils.Formatter.YYYY_MM_DD;
    private DateTimeUtils.Formatter dateTimeFormatter = DateTimeUtils.Formatter.YYYY_MM_DD_HH_MM_SS;
    private TimeUtils.Formatter timeFormatter = TimeUtils.Formatter.HH_MM_SS;

    protected Object convert(SeaTunnelDataType dataType, Object val) {
        if (val == null) {
            return null;
        }
        switch (dataType.getSqlType()) {
            case TINYINT: 
            case SMALLINT: 
            case INT: 
            case BIGINT: 
            case FLOAT: 
            case DOUBLE: 
            case DECIMAL: 
            case BOOLEAN: 
            case STRING: {
                return val;
            }
            case DATE: {
                return DateUtils.toString((LocalDate)val, this.dateFormatter);
            }
            case TIME: {
                return TimeUtils.toString((LocalTime)val, this.timeFormatter);
            }
            case TIMESTAMP: {
                return DateTimeUtils.toString((LocalDateTime)val, this.dateTimeFormatter);
            }
            case ARRAY: 
            case MAP: {
                return JsonUtils.toJsonString(val);
            }
            case BYTES: {
                return new String((byte[])val);
            }
        }
        throw new DorisConnectorException((SeaTunnelErrorCode)CommonErrorCode.UNSUPPORTED_DATA_TYPE, dataType + " is not supported ");
    }
}

