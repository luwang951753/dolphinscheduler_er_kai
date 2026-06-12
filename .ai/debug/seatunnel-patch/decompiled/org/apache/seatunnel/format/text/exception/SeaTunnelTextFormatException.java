/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.format.text.exception;

import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelRuntimeException;

public class SeaTunnelTextFormatException
extends SeaTunnelRuntimeException {
    public SeaTunnelTextFormatException(SeaTunnelErrorCode seaTunnelErrorCode, String errorMessage) {
        super(seaTunnelErrorCode, errorMessage);
    }

    public SeaTunnelTextFormatException(SeaTunnelErrorCode seaTunnelErrorCode, String errorMessage, Throwable cause) {
        super(seaTunnelErrorCode, errorMessage, cause);
    }

    public SeaTunnelTextFormatException(SeaTunnelErrorCode seaTunnelErrorCode, Throwable cause) {
        super(seaTunnelErrorCode, cause);
    }
}

