/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordsWithSplitIds;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.SplitFetcher;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SplitFetcherManager<E, SplitT extends SourceSplit> {
    private static final Logger log = LoggerFactory.getLogger(SplitFetcherManager.class);
    protected final Map<Integer, SplitFetcher<E, SplitT>> fetchers = new ConcurrentHashMap<Integer, SplitFetcher<E, SplitT>>();
    private final BlockingQueue<RecordsWithSplitIds<E>> elementsQueue;
    private final Supplier<SplitReader<E, SplitT>> splitReaderFactory;
    private final Consumer<Collection<String>> splitFinishedHook;
    private final AtomicInteger fetcherIdGenerator;
    private final AtomicReference<Throwable> uncaughtFetcherException;
    private final Consumer<Throwable> errorHandler;
    private final ExecutorService executors;
    private volatile boolean closed;

    public SplitFetcherManager(BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, Supplier<SplitReader<E, SplitT>> splitReaderFactory) {
        this(elementsQueue, splitReaderFactory, ignore -> {});
    }

    public SplitFetcherManager(BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, Supplier<SplitReader<E, SplitT>> splitReaderFactory, Consumer<Collection<String>> splitFinishedHook) {
        this.elementsQueue = elementsQueue;
        this.splitReaderFactory = splitReaderFactory;
        this.splitFinishedHook = splitFinishedHook;
        this.fetcherIdGenerator = new AtomicInteger(0);
        this.uncaughtFetcherException = new AtomicReference<Object>(null);
        this.errorHandler = throwable -> {
            log.error("Received uncaught exception.", (Throwable)throwable);
            if (!this.uncaughtFetcherException.compareAndSet((Throwable)null, (Throwable)throwable)) {
                this.uncaughtFetcherException.get().addSuppressed((Throwable)throwable);
            }
        };
        String taskThreadName = Thread.currentThread().getName();
        this.executors = Executors.newCachedThreadPool(r -> new Thread(r, "Source Data Fetcher for " + taskThreadName));
    }

    public abstract void addSplits(Collection<SplitT> var1);

    protected void startFetcher(SplitFetcher<E, SplitT> fetcher) {
        this.executors.submit(fetcher);
    }

    protected synchronized SplitFetcher<E, SplitT> createSplitFetcher() {
        if (this.closed) {
            throw new IllegalStateException("The split fetcher manager has closed.");
        }
        SplitReader<E, SplitT> splitReader = this.splitReaderFactory.get();
        int fetcherId = this.fetcherIdGenerator.getAndIncrement();
        SplitFetcher<E, SplitT> splitFetcher = new SplitFetcher<E, SplitT>(fetcherId, this.elementsQueue, splitReader, this.errorHandler, () -> this.fetchers.remove(fetcherId), this.splitFinishedHook);
        this.fetchers.put(fetcherId, splitFetcher);
        return splitFetcher;
    }

    public synchronized boolean maybeShutdownFinishedFetchers() {
        Iterator<Map.Entry<Integer, SplitFetcher<E, SplitT>>> iter = this.fetchers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, SplitFetcher<E, SplitT>> entry = iter.next();
            SplitFetcher<E, SplitT> fetcher = entry.getValue();
            if (!fetcher.isIdle()) continue;
            log.info("Closing splitFetcher {} because it is idle.", (Object)entry.getKey());
            fetcher.shutdown();
            iter.remove();
        }
        return this.fetchers.isEmpty();
    }

    public synchronized void close(long timeoutMs) throws Exception {
        this.closed = true;
        this.fetchers.values().forEach(SplitFetcher::shutdown);
        this.executors.shutdown();
        if (!this.executors.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
            log.warn("Failed to close the source reader in {} ms. There are still {} split fetchers running", (Object)timeoutMs, (Object)this.fetchers.size());
        }
    }

    public void checkErrors() {
        if (this.uncaughtFetcherException.get() != null) {
            throw new RuntimeException("One or more fetchers have encountered exception", this.uncaughtFetcherException.get());
        }
    }
}

