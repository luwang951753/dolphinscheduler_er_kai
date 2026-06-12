/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordEmitter;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordsWithSplitIds;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.SourceReaderBase;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.SourceReaderOptions;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.SingleThreadFetcherManager;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitReader;

public abstract class SingleThreadMultiplexSourceReaderBase<E, T, SplitT extends SourceSplit, SplitStateT>
extends SourceReaderBase<E, T, SplitT, SplitStateT> {
    public SingleThreadMultiplexSourceReaderBase(Supplier<SplitReader<E, SplitT>> splitReaderSupplier, RecordEmitter<E, T, SplitStateT> recordEmitter, SourceReaderOptions options, SourceReader.Context context) {
        this(new ArrayBlockingQueue<RecordsWithSplitIds<E>>(options.getElementQueueCapacity()), splitReaderSupplier, recordEmitter, options, context);
    }

    public SingleThreadMultiplexSourceReaderBase(BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, Supplier<SplitReader<E, SplitT>> splitReaderSupplier, RecordEmitter<E, T, SplitStateT> recordEmitter, SourceReaderOptions options, SourceReader.Context context) {
        super(elementsQueue, new SingleThreadFetcherManager<E, SplitT>(elementsQueue, splitReaderSupplier), recordEmitter, options, context);
    }

    public SingleThreadMultiplexSourceReaderBase(BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, SingleThreadFetcherManager<E, SplitT> splitFetcherManager, RecordEmitter<E, T, SplitStateT> recordEmitter, SourceReaderOptions options, SourceReader.Context context) {
        super(elementsQueue, splitFetcherManager, recordEmitter, options, context);
    }
}

