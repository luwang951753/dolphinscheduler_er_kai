/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader;

import java.io.IOException;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordsWithSplitIds;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitsChange;

public interface SplitReader<E, SplitT extends SourceSplit> {
    public RecordsWithSplitIds<E> fetch() throws IOException;

    public void handleSplitsChanges(SplitsChange<SplitT> var1);

    public void wakeUp();

    public void close() throws Exception;
}

