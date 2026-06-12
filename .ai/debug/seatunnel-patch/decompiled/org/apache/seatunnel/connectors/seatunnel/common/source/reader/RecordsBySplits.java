/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordsWithSplitIds;

public class RecordsBySplits<E>
implements RecordsWithSplitIds<E> {
    private final Set<String> finishedSplits;
    private final Iterator<Map.Entry<String, Collection<E>>> splitsIterator;
    private Iterator<E> recordsInCurrentSplit;

    public RecordsBySplits(Map<String, Collection<E>> recordsBySplit, Set<String> finishedSplits) {
        this.splitsIterator = Preconditions.checkNotNull(recordsBySplit, "recordsBySplit").entrySet().iterator();
        this.finishedSplits = Preconditions.checkNotNull(finishedSplits, "finishedSplits");
    }

    @Override
    public String nextSplit() {
        if (this.splitsIterator.hasNext()) {
            Map.Entry<String, Collection<E>> next = this.splitsIterator.next();
            this.recordsInCurrentSplit = next.getValue().iterator();
            return next.getKey();
        }
        return null;
    }

    @Override
    public E nextRecordFromSplit() {
        if (this.recordsInCurrentSplit == null) {
            throw new IllegalStateException();
        }
        return this.recordsInCurrentSplit.hasNext() ? (E)this.recordsInCurrentSplit.next() : null;
    }

    @Override
    public Set<String> finishedSplits() {
        return this.finishedSplits;
    }
}

