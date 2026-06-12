/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.exception;

import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;

public enum DorisConnectorErrorCode implements SeaTunnelErrorCode
{
    STREAM_LOAD_FAILED("Doris-01", "stream load error"),
    COMMIT_FAILED("Doris-02", "commit error"),
    REST_SERVICE_FAILED("Doris-03", "rest service error");

    private final String code;
    private final String description;

    private DorisConnectorErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}

