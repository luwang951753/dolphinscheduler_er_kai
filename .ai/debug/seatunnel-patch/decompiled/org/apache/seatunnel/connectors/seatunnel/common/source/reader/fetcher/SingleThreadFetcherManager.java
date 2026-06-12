/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordsWithSplitIds;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.SplitFetcher;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.SplitFetcherManager;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitReader;

public class SingleThreadFetcherManager<E, SplitT extends SourceSplit>
extends SplitFetcherManager<E, SplitT> {
    public SingleThreadFetcherManager(BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, Supplier<SplitReader<E, SplitT>> splitReaderSupplier) {
        super(elementsQueue, splitReaderSupplier);
    }

    public SingleThreadFetcherManager(BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, Supplier<SplitReader<E, SplitT>> splitReaderSupplier, Consumer<Collection<String>> splitFinishedHook) {
        super(elementsQueue, splitReaderSupplier, splitFinishedHook);
    }

    @Override
    public void addSplits(Collection<SplitT> splitsToAdd) {
        SplitFetcher<E, SplitT> fetcher = this.getRunningFetcher();
        if (fetcher == null) {
            fetcher = this.createSplitFetcher();
            fetcher.addSplits(splitsToAdd);
            this.startFetcher(fetcher);
        } else {
            fetcher.addSplits(splitsToAdd);
        }
    }

    protected SplitFetcher<E, SplitT> getRunningFetcher() {
        return this.fetchers.isEmpty() ? null : (SplitFetcher)this.fetchers.values().iterator().next();
    }
}

