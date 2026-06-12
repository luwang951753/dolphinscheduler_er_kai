/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.seatunnel.api.source.Boundedness;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.source.SourceEvent;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.common.utils.SeaTunnelException;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordEmitter;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordsWithSplitIds;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.SourceReaderOptions;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.SplitFetcherManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SourceReaderBase<E, T, SplitT extends SourceSplit, SplitStateT>
implements SourceReader<T, SplitT> {
    private static final Logger log = LoggerFactory.getLogger(SourceReaderBase.class);
    private final BlockingQueue<RecordsWithSplitIds<E>> elementsQueue;
    private final ConcurrentMap<String, SplitContext<T, SplitStateT>> splitStates;
    protected final RecordEmitter<E, T, SplitStateT> recordEmitter;
    protected final SplitFetcherManager<E, SplitT> splitFetcherManager;
    protected final SourceReaderOptions options;
    protected final SourceReader.Context context;
    private RecordsWithSplitIds<E> currentFetch;
    private SplitContext<T, SplitStateT> currentSplitContext;
    private Collector<T> currentSplitOutput;
    private boolean noMoreSplitsAssignment;

    public SourceReaderBase(BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, SplitFetcherManager<E, SplitT> splitFetcherManager, RecordEmitter<E, T, SplitStateT> recordEmitter, SourceReaderOptions options, SourceReader.Context context) {
        this.elementsQueue = elementsQueue;
        this.splitFetcherManager = splitFetcherManager;
        this.recordEmitter = recordEmitter;
        this.splitStates = new ConcurrentHashMap<String, SplitContext<T, SplitStateT>>();
        this.options = options;
        this.context = context;
    }

    @Override
    public void open() {
        log.info("Open Source Reader.");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void pollNext(Collector<T> output) throws Exception {
        RecordsWithSplitIds<E> recordsWithSplitId = this.currentFetch;
        if (recordsWithSplitId == null && (recordsWithSplitId = this.getNextFetch(output)) == null) {
            if (Boundedness.BOUNDED.equals((Object)this.context.getBoundedness()) && this.noMoreSplitsAssignment && this.splitFetcherManager.maybeShutdownFinishedFetchers() && this.elementsQueue.isEmpty()) {
                this.context.signalNoMoreElement();
                log.info("Send NoMoreElement event");
            }
            return;
        }
        E record = recordsWithSplitId.nextRecordFromSplit();
        if (record != null) {
            Object object = output.getCheckpointLock();
            synchronized (object) {
                this.recordEmitter.emitRecord(record, this.currentSplitOutput, this.currentSplitContext.state);
            }
            log.trace("Emitted record: {}", (Object)record);
        } else if (!this.moveToNextSplit(recordsWithSplitId, output)) {
            this.pollNext(output);
        }
    }

    @Override
    public List<SplitT> snapshotState(long checkpointId) {
        ArrayList splits = new ArrayList();
        this.splitStates.forEach((id, context) -> splits.add(this.toSplitType((String)id, context.state)));
        log.debug("Snapshot state from splits: {}", (Object)splits);
        return splits;
    }

    @Override
    public void addSplits(List<SplitT> splits) {
        log.debug("Adding split(s) to reader: {}", (Object)splits);
        splits.forEach(split -> this.splitStates.put(split.splitId(), new SplitContext(split.splitId(), this.initializedState(split))));
        this.splitFetcherManager.addSplits(splits);
    }

    @Override
    public void handleNoMoreSplits() {
        log.info("Reader received NoMoreSplits event.");
        this.noMoreSplitsAssignment = true;
    }

    @Override
    public void handleSourceEvent(SourceEvent sourceEvent) {
        log.info("Received unhandled source event: {}", (Object)sourceEvent);
    }

    @Override
    public void close() {
        log.info("Closing Source Reader.");
        try {
            this.splitFetcherManager.close(this.options.getSourceReaderCloseTimeout());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RecordsWithSplitIds<E> getNextFetch(Collector<T> output) {
        this.splitFetcherManager.checkErrors();
        RecordsWithSplitIds recordsWithSplitId = (RecordsWithSplitIds)this.elementsQueue.poll();
        if (recordsWithSplitId == null || !this.moveToNextSplit(recordsWithSplitId, output)) {
            try {
                log.trace("Current fetch is finished.");
                Thread.sleep(100L);
            }
            catch (InterruptedException e) {
                throw new SeaTunnelException(e);
            }
            return null;
        }
        this.currentFetch = recordsWithSplitId;
        return recordsWithSplitId;
    }

    private boolean moveToNextSplit(RecordsWithSplitIds<E> recordsWithSplitIds, Collector<T> output) {
        String nextSplitId = recordsWithSplitIds.nextSplit();
        if (nextSplitId == null) {
            log.trace("Current fetch is finished.");
            this.finishCurrentFetch(recordsWithSplitIds, output);
            return false;
        }
        this.currentSplitContext = (SplitContext)this.splitStates.get(nextSplitId);
        Preconditions.checkState(this.currentSplitContext != null, "Have records for a split that was not registered");
        this.currentSplitOutput = this.currentSplitContext.getOrCreateSplitOutput(output);
        log.trace("Emitting records from fetch for split {}", (Object)nextSplitId);
        return true;
    }

    private void finishCurrentFetch(RecordsWithSplitIds<E> fetch, Collector<T> output) {
        this.currentFetch = null;
        this.currentSplitContext = null;
        this.currentSplitOutput = null;
        Set<String> finishedSplits = fetch.finishedSplits();
        if (!finishedSplits.isEmpty()) {
            log.info("Finished reading split(s) {}", (Object)finishedSplits);
            HashMap stateOfFinishedSplits = new HashMap();
            for (String finishedSplitId : finishedSplits) {
                stateOfFinishedSplits.put(finishedSplitId, ((SplitContext)this.splitStates.remove((Object)finishedSplitId)).state);
            }
            this.onSplitFinished(stateOfFinishedSplits);
        }
        fetch.recycle();
    }

    public int getNumberOfCurrentlyAssignedSplits() {
        return this.splitStates.size();
    }

    protected abstract void onSplitFinished(Map<String, SplitStateT> var1);

    protected abstract SplitStateT initializedState(SplitT var1);

    protected abstract SplitT toSplitType(String var1, SplitStateT var2);

    private static final class SplitContext<T, SplitStateT> {
        final String splitId;
        final SplitStateT state;
        Collector<T> splitOutput;

        Collector<T> getOrCreateSplitOutput(Collector<T> output) {
            if (this.splitOutput == null) {
                this.splitOutput = output;
            }
            return this.splitOutput;
        }

        public SplitContext(String splitId, SplitStateT state) {
            this.splitId = splitId;
            this.state = state;
        }
    }
}

