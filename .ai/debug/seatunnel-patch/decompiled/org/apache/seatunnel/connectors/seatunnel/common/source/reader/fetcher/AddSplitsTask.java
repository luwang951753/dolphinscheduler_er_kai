/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher;

import java.util.Collection;
import java.util.Map;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.SplitFetcherTask;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitReader;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitsAddition;

class AddSplitsTask<SplitT extends SourceSplit>
implements SplitFetcherTask {
    private final SplitReader<?, SplitT> splitReader;
    private final Collection<SplitT> splitsToAdd;
    private final Map<String, SplitT> assignedSplits;

    @Override
    public void run() {
        for (SourceSplit s : this.splitsToAdd) {
            this.assignedSplits.put(s.splitId(), s);
        }
        this.splitReader.handleSplitsChanges(new SplitsAddition<SplitT>(this.splitsToAdd));
    }

    @Override
    public void wakeUp() {
    }

    public AddSplitsTask(SplitReader<?, SplitT> splitReader, Collection<SplitT> splitsToAdd, Map<String, SplitT> assignedSplits) {
        this.splitReader = splitReader;
        this.splitsToAdd = splitsToAdd;
        this.assignedSplits = assignedSplits;
    }

    public String toString() {
        return "AddSplitsTask(splitsToAdd=" + this.splitsToAdd + ")";
    }
}

