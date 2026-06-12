/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.json.exception;

import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelRuntimeException;

public class SeaTunnelJsonFormatException
extends SeaTunnelRuntimeException {
    public SeaTunnelJsonFormatException(SeaTunnelErrorCode seaTunnelErrorCode, String errorMessage) {
        super(seaTunnelErrorCode, errorMessage);
    }

    public SeaTunnelJsonFormatException(SeaTunnelErrorCode seaTunnelErrorCode, String errorMessage, Throwable cause) {
        super(seaTunnelErrorCode, errorMessage, cause);
    }

    public SeaTunnelJsonFormatException(SeaTunnelErrorCode seaTunnelErrorCode, Throwable cause) {
        super(seaTunnelErrorCode, cause);
    }
}

