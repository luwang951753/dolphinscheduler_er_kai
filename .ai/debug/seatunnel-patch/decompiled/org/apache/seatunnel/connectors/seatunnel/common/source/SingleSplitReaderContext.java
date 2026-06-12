/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source;

import org.apache.seatunnel.api.source.Boundedness;
import org.apache.seatunnel.api.source.SourceReader;

public class SingleSplitReaderContext {
    private final SourceReader.Context context;

    public SingleSplitReaderContext(SourceReader.Context context) {
        this.context = context;
    }

    public Boundedness getBoundedness() {
        return this.context.getBoundedness();
    }

    public void signalNoMoreElement() {
        this.context.signalNoMoreElement();
    }
}

