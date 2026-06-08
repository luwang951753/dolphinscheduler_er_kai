/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.api.service;

import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLXML;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class DataPreviewCellValueNormalizer {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DataPreviewCellValueNormalizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Object normalize(Object value) {
        if (value == null
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof String) {
            return value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().format(TIMESTAMP_FORMATTER);
        }
        if (value instanceof java.sql.Date || value instanceof java.sql.Time || value instanceof Date) {
            return value.toString();
        }
        if (value instanceof byte[]) {
            return "<binary:" + ((byte[]) value).length + " bytes>";
        }
        if (value instanceof Blob) {
            return blobSummary((Blob) value);
        }
        if (value instanceof Clob) {
            return clobSummary((Clob) value);
        }
        if (value instanceof Array) {
            return sqlArraySummary((Array) value);
        }
        if (value instanceof SQLXML) {
            return sqlXmlString((SQLXML) value);
        }

        Object reflectedValue = invokeNoArg(value, "timestampValue");
        if (reflectedValue instanceof Timestamp) {
            return ((Timestamp) reflectedValue).toLocalDateTime().format(TIMESTAMP_FORMATTER);
        }
        reflectedValue = invokeNoArg(value, "dateValue");
        if (reflectedValue instanceof Date) {
            return reflectedValue.toString();
        }
        reflectedValue = invokeNoArg(value, "stringValue");
        if (reflectedValue instanceof String) {
            return reflectedValue;
        }
        reflectedValue = invokeNoArg(value, "toJdbc");
        if (reflectedValue != null && reflectedValue != value) {
            return normalize(reflectedValue);
        }
        return value.toString();
    }

    private static Object invokeNoArg(Object value, String methodName) {
        try {
            Method method = value.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String blobSummary(Blob blob) {
        try {
            return "<blob:" + blob.length() + " bytes>";
        } catch (Exception ex) {
            return "<blob>";
        }
    }

    private static String clobSummary(Clob clob) {
        try {
            long length = clob.length();
            int readLength = (int) Math.min(length, 4000);
            return clob.getSubString(1, readLength);
        } catch (Exception ex) {
            return "<clob>";
        }
    }

    private static String sqlArraySummary(Array array) {
        try {
            Object value = array.getArray();
            return value == null ? null : value.toString();
        } catch (Exception ex) {
            return "<array>";
        }
    }

    private static String sqlXmlString(SQLXML sqlxml) {
        try {
            return sqlxml.getString();
        } catch (Exception ex) {
            return "<sqlxml>";
        }
    }
}
