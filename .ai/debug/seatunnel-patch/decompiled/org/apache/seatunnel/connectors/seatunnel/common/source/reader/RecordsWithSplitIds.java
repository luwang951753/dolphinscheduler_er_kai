/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader;

import java.util.Set;

public interface RecordsWithSplitIds<E> {
    public String nextSplit();

    public E nextRecordFromSplit();

    public Set<String> finishedSplits();

    default public void recycle() {
    }
}

