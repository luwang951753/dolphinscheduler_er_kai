/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source;

import org.apache.seatunnel.api.serialization.DefaultSerializer;
import org.apache.seatunnel.api.serialization.Serializer;
import org.apache.seatunnel.api.source.SeaTunnelSource;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.api.source.SourceSplitEnumerator;
import org.apache.seatunnel.connectors.seatunnel.common.source.AbstractSingleSplitReader;
import org.apache.seatunnel.connectors.seatunnel.common.source.SingleSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.SingleSplitEnumerator;
import org.apache.seatunnel.connectors.seatunnel.common.source.SingleSplitEnumeratorState;
import org.apache.seatunnel.connectors.seatunnel.common.source.SingleSplitReaderContext;
import org.apache.seatunnel.shade.com.google.common.base.Preconditions;

public abstract class AbstractSingleSplitSource<T>
implements SeaTunnelSource<T, SingleSplit, SingleSplitEnumeratorState> {
    public final AbstractSingleSplitReader<T> createReader(SourceReader.Context readerContext) throws Exception {
        Preconditions.checkArgument(readerContext.getIndexOfSubtask() == 0, "A single split source allows only one single reader to be created. Please make sure source parallelism = 1");
        return this.createReader(new SingleSplitReaderContext(readerContext));
    }

    public abstract AbstractSingleSplitReader<T> createReader(SingleSplitReaderContext var1) throws Exception;

    @Override
    public final SourceSplitEnumerator<SingleSplit, SingleSplitEnumeratorState> createEnumerator(SourceSplitEnumerator.Context<SingleSplit> enumeratorContext) throws Exception {
        return new SingleSplitEnumerator(enumeratorContext);
    }

    @Override
    public final SourceSplitEnumerator<SingleSplit, SingleSplitEnumeratorState> restoreEnumerator(SourceSplitEnumerator.Context<SingleSplit> enumeratorContext, SingleSplitEnumeratorState checkpointState) throws Exception {
        return this.createEnumerator(enumeratorContext);
    }

    @Override
    public final Serializer<SingleSplit> getSplitSerializer() {
        return new DefaultSerializer<SingleSplit>();
    }
}

