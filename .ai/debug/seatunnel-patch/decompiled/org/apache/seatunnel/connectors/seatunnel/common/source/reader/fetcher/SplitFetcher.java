/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.NonNull
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.NonNull;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.RecordsWithSplitIds;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.AddSplitsTask;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.FetchTask;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.fetcher.SplitFetcherTask;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitFetcher<E, SplitT extends SourceSplit>
implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SplitFetcher.class);
    private final int fetcherId;
    private final Deque<SplitFetcherTask> taskQueue = new ArrayDeque<SplitFetcherTask>();
    private final Map<String, SplitT> assignedSplits = new HashMap<String, SplitT>();
    private final SplitReader<E, SplitT> splitReader;
    private final Consumer<Throwable> errorHandler;
    private final Runnable shutdownHook;
    private final FetchTask fetchTask;
    private volatile boolean closed;
    private volatile SplitFetcherTask runningTask = null;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition nonEmpty = this.lock.newCondition();

    SplitFetcher(int fetcherId, @NonNull BlockingQueue<RecordsWithSplitIds<E>> elementsQueue, @NonNull SplitReader<E, SplitT> splitReader, @NonNull Consumer<Throwable> errorHandler, @NonNull Runnable shutdownHook, @NonNull Consumer<Collection<String>> splitFinishedHook) {
        if (elementsQueue == null) {
            throw new NullPointerException("elementsQueue is marked non-null but is null");
        }
        if (splitReader == null) {
            throw new NullPointerException("splitReader is marked non-null but is null");
        }
        if (errorHandler == null) {
            throw new NullPointerException("errorHandler is marked non-null but is null");
        }
        if (shutdownHook == null) {
            throw new NullPointerException("shutdownHook is marked non-null but is null");
        }
        if (splitFinishedHook == null) {
            throw new NullPointerException("splitFinishedHook is marked non-null but is null");
        }
        this.fetcherId = fetcherId;
        this.splitReader = splitReader;
        this.errorHandler = errorHandler;
        this.shutdownHook = shutdownHook;
        this.fetchTask = new FetchTask<E, SplitT>(splitReader, elementsQueue, finishedSplits -> {
            finishedSplits.forEach(this.assignedSplits::remove);
            splitFinishedHook.accept((Collection<String>)finishedSplits);
            log.info("Finished reading from splits {}", finishedSplits);
        }, fetcherId);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        log.info("Starting split fetcher {}", (Object)this.fetcherId);
        try {
            while (this.runOnce()) {
            }
        }
        catch (Throwable t) {
            this.errorHandler.accept(t);
        }
        finally {
            try {
                this.splitReader.close();
            }
            catch (Exception e) {
                this.errorHandler.accept(e);
            }
            finally {
                log.info("Split fetcher {} exited.", (Object)this.fetcherId);
                this.shutdownHook.run();
            }
        }
    }

    public void addSplits(@NonNull Collection<SplitT> splitsToAdd) {
        if (splitsToAdd == null) {
            throw new NullPointerException("splitsToAdd is marked non-null but is null");
        }
        this.lock.lock();
        try {
            this.addTaskUnsafe(new AddSplitsTask<SplitT>(this.splitReader, splitsToAdd, this.assignedSplits));
            this.wakeUpUnsafe(true);
        }
        finally {
            this.lock.unlock();
        }
    }

    public void addTask(@NonNull SplitFetcherTask task) {
        if (task == null) {
            throw new NullPointerException("task is marked non-null but is null");
        }
        this.lock.lock();
        try {
            this.addTaskUnsafe(task);
        }
        finally {
            this.lock.unlock();
        }
    }

    public void shutdown() {
        this.lock.lock();
        try {
            if (!this.closed) {
                this.closed = true;
                log.info("Shutting down split fetcher {}", (Object)this.fetcherId);
                this.wakeUpUnsafe(false);
            }
        }
        finally {
            this.lock.unlock();
        }
    }

    public boolean isIdle() {
        this.lock.lock();
        try {
            boolean bl = this.assignedSplits.isEmpty() && this.taskQueue.isEmpty() && this.runningTask == null;
            return bl;
        }
        finally {
            this.lock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean runOnce() {
        SplitFetcherTask nextTask;
        this.lock.lock();
        try {
            if (this.closed) {
                boolean bl = false;
                return bl;
            }
            nextTask = this.getNextTaskUnsafe();
            if (nextTask == null) {
                boolean bl = true;
                return bl;
            }
            log.debug("Prepare to run {}", (Object)nextTask);
            this.runningTask = nextTask;
        }
        finally {
            this.lock.unlock();
        }
        try {
            nextTask.run();
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("SplitFetcher thread %d received unexpected exception while polling the records", this.fetcherId), e);
        }
        this.lock.lock();
        try {
            this.runningTask = null;
        }
        finally {
            this.lock.unlock();
        }
        return true;
    }

    private SplitFetcherTask getNextTaskUnsafe() {
        assert (this.lock.isHeldByCurrentThread());
        try {
            if (!this.taskQueue.isEmpty()) {
                return this.taskQueue.poll();
            }
            if (!this.assignedSplits.isEmpty()) {
                return this.fetchTask;
            }
            this.nonEmpty.await();
            return this.taskQueue.poll();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("The thread was interrupted while waiting for a fetcher task.");
        }
    }

    private void wakeUpUnsafe(boolean taskOnly) {
        assert (this.lock.isHeldByCurrentThread());
        SplitFetcherTask currentTask = this.runningTask;
        if (currentTask != null) {
            log.debug("Waking up running task {}", (Object)currentTask);
            currentTask.wakeUp();
        } else if (!taskOnly) {
            log.debug("Waking up fetcher thread.");
            this.nonEmpty.signal();
        }
    }

    private void addTaskUnsafe(SplitFetcherTask task) {
        assert (this.lock.isHeldByCurrentThread());
        this.taskQueue.add(task);
        this.nonEmpty.signal();
    }

    public int getFetcherId() {
        return this.fetcherId;
    }

    public Map<String, SplitT> getAssignedSplits() {
        return this.assignedSplits;
    }

    public SplitReader<E, SplitT> getSplitReader() {
        return this.splitReader;
    }
}

