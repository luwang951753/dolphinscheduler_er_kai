/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.sink;

import java.util.Optional;
import org.apache.seatunnel.api.sink.SinkWriter;

public abstract class AbstractSinkWriter<T, StateT>
implements SinkWriter<T, Void, StateT> {
    @Override
    public Optional<Void> prepareCommit() {
        return Optional.empty();
    }

    @Override
    public final void abortPrepare() {
    }
}

