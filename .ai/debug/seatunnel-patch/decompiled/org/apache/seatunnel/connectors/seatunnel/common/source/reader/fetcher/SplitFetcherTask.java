/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher;

import java.io.IOException;

public interface SplitFetcherTask {
    public void run() throws IOException;

    public void wakeUp();
}

