/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.serialize;

import java.io.IOException;
import java.io.Serializable;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;

public interface DorisSerializer
extends Serializable {
    public void open() throws IOException;

    public byte[] serialize(SeaTunnelRow var1) throws IOException;

    public void close() throws IOException;
}

