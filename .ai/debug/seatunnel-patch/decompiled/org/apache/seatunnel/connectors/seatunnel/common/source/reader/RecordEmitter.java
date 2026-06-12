/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader;

import org.apache.seatunnel.api.source.Collector;

public interface RecordEmitter<E, T, SplitStateT> {
    public void emitRecord(E var1, Collector<T> var2, SplitStateT var3) throws Exception;
}

