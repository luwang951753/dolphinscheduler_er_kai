/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.sink;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.seatunnel.api.serialization.Serializer;
import org.apache.seatunnel.api.sink.SeaTunnelSink;
import org.apache.seatunnel.api.sink.SinkAggregatedCommitter;
import org.apache.seatunnel.api.sink.SinkCommitter;
import org.apache.seatunnel.api.sink.SinkWriter;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSinkWriter;

public abstract class AbstractSimpleSink<T, StateT>
implements SeaTunnelSink<T, StateT, Void, Void> {
    public abstract AbstractSinkWriter<T, StateT> createWriter(SinkWriter.Context var1) throws IOException;

    @Override
    public SinkWriter<T, Void, StateT> restoreWriter(SinkWriter.Context context, List<StateT> states) throws IOException {
        return this.createWriter(context);
    }

    @Override
    public final Optional<SinkCommitter<Void>> createCommitter() throws IOException {
        return Optional.empty();
    }

    @Override
    public final Optional<Serializer<Void>> getCommitInfoSerializer() {
        return Optional.empty();
    }

    @Override
    public final Optional<SinkAggregatedCommitter<Void, Void>> createAggregatedCommitter() throws IOException {
        return Optional.empty();
    }

    @Override
    public final Optional<Serializer<Void>> getAggregatedCommitInfoSerializer() {
        return Optional.empty();
    }
}

