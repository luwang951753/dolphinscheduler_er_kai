/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source;

import java.util.Collections;
import java.util.List;
import org.apache.seatunnel.api.source.SourceEvent;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.connectors.seatunnel.common.source.SingleSplit;

public abstract class AbstractSingleSplitReader<T>
implements SourceReader<T, SingleSplit> {
    @Override
    public final List<SingleSplit> snapshotState(long checkpointId) throws Exception {
        return Collections.singletonList(new SingleSplit(this.snapshotStateToBytes(checkpointId)));
    }

    protected byte[] snapshotStateToBytes(long checkpointId) throws Exception {
        return null;
    }

    @Override
    public final void addSplits(List<SingleSplit> splits) {
        if (splits.size() > 1) {
            throw new UnsupportedOperationException("The single-split reader don't support reading multiple splits");
        }
        byte[] restoredState = splits.get(0).getState();
        if (restoredState != null && restoredState.length > 0) {
            this.restoreState(restoredState);
        }
    }

    protected void restoreState(byte[] restoredState) {
    }

    @Override
    public final void handleNoMoreSplits() {
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {
    }

    @Override
    public final void handleSourceEvent(SourceEvent sourceEvent) {
    }
}

