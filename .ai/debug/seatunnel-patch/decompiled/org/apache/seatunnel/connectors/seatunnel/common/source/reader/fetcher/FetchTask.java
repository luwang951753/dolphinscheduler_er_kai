/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordsWithSplitIds;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.SplitFetcherTask;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FetchTask<E, SplitT extends SourceSplit>
implements SplitFetcherTask {
    private static final Logger log = LoggerFactory.getLogger(FetchTask.class);
    private static final int OFFER_TIMEOUT_MILLIS = 10000;
    private final SplitReader<E, SplitT> splitReader;
    private final BlockingQueue<RecordsWithSplitIds<E>> elementsQueue;
    private final Consumer<Collection<String>> splitFinishedCallback;
    private final int fetcherIndex;
    private volatile boolean wakeup;
    private volatile RecordsWithSplitIds<E> lastRecords;

    @Override
    public void run() throws IOException {
        try {
            if (!this.isWakeup() && this.lastRecords == null) {
                this.lastRecords = this.splitReader.fetch();
                log.debug("Fetch records from split fetcher {}", (Object)this.fetcherIndex);
            }
            if (!this.isWakeup()) {
                if (this.elementsQueue.offer(this.lastRecords, 10000L, TimeUnit.MILLISECONDS)) {
                    if (!this.lastRecords.finishedSplits().isEmpty()) {
                        this.splitFinishedCallback.accept(this.lastRecords.finishedSplits());
                    }
                    this.lastRecords = null;
                    log.debug("Enqueued records from split fetcher {}", (Object)this.fetcherIndex);
                } else {
                    log.debug("Enqueuing timed out in split fetcher {}, queue is blocked", (Object)this.fetcherIndex);
                }
            }
        }
        catch (IOException | InterruptedException e) {
            throw new IOException("Source fetch execution was fail", e);
        }
        finally {
            if (this.isWakeup()) {
                this.wakeup = false;
            }
        }
    }

    @Override
    public void wakeUp() {
        this.wakeup = true;
        if (this.lastRecords == null) {
            this.splitReader.wakeUp();
        }
    }

    public FetchTask(SplitReader<E, SplitT> splitReader, BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, Consumer<Collection<String>> splitFinishedCallback, int fetcherIndex) {
        this.splitReader = splitReader;
        this.elementsQueue = elementsQueue;
        this.splitFinishedCallback = splitFinishedCallback;
        this.fetcherIndex = fetcherIndex;
    }

    private boolean isWakeup() {
        return this.wakeup;
    }
}

