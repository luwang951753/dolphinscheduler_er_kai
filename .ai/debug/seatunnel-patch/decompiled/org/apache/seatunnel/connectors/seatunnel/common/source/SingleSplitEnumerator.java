/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.apache.seatunnel.api.source.SourceSplitEnumerator;
import org.apache.seatunnel.connectors.seatunnel.common.source.SingleSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.SingleSplitEnumeratorState;

public class SingleSplitEnumerator
implements SourceSplitEnumerator<SingleSplit, SingleSplitEnumeratorState> {
    protected final SourceSplitEnumerator.Context<SingleSplit> context;
    protected SingleSplit pendingSplit;
    protected volatile boolean assigned = false;

    public SingleSplitEnumerator(SourceSplitEnumerator.Context<SingleSplit> context) {
        this.context = context;
    }

    @Override
    public void open() {
    }

    @Override
    public void run() throws Exception {
        if (this.assigned || this.pendingSplit != null) {
            return;
        }
        this.pendingSplit = new SingleSplit(null);
        this.assignSplit();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void addSplitsBack(List<SingleSplit> splits, int subtaskId) {
        this.pendingSplit = splits.get(0);
        this.assignSplit();
    }

    protected void assignSplit() {
        if (this.assigned || this.pendingSplit == null) {
            return;
        }
        Set<Integer> readers = this.context.registeredReaders();
        if (!readers.isEmpty()) {
            this.context.assignSplit((int)((Integer)readers.stream().findFirst().get()), this.pendingSplit);
            this.assigned = true;
        }
    }

    @Override
    public int currentUnassignedSplitSize() {
        return 0;
    }

    @Override
    public void handleSplitRequest(int subtaskId) {
    }

    @Override
    public void registerReader(int subtaskId) {
        this.assignSplit();
    }

    @Override
    public SingleSplitEnumeratorState snapshotState(long checkpointId) throws Exception {
        return new SingleSplitEnumeratorState();
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {
    }
}

