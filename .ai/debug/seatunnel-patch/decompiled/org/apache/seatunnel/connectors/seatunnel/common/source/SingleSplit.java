/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source;

import org.apache.seatunnel.api.source.SourceSplit;

public class SingleSplit
implements SourceSplit {
    private final byte[] state;

    public SingleSplit(byte[] state) {
        this.state = state;
    }

    public byte[] getState() {
        return this.state;
    }

    @Override
    public String splitId() {
        return "single";
    }
}

