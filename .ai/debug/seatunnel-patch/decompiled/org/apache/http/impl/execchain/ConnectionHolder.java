/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.execchain;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.http.HttpClientConnection;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.conn.ConnectionReleaseTrigger;
import org.apache.http.conn.HttpClientConnectionManager;

@Contract(threading=ThreadingBehavior.SAFE)
class ConnectionHolder
implements ConnectionReleaseTrigger,
Cancellable,
Closeable {
    private final Log log;
    private final HttpClientConnectionManager manager;
    private final HttpClientConnection managedConn;
    private final AtomicBoolean released;
    private volatile boolean reusable;
    private volatile Object state;
    private volatile long validDuration;
    private volatile TimeUnit timeUnit;

    public ConnectionHolder(Log log, HttpClientConnectionManager manager, HttpClientConnection managedConn) {
        this.log = log;
        this.manager = manager;
        this.managedConn = managedConn;
        this.released = new AtomicBoolean(false);
    }

    public boolean isReusable() {
        return this.reusable;
    }

    public void markReusable() {
        this.reusable = true;
    }

    public void markNonReusable() {
        this.reusable = false;
    }

    public void setState(Object state) {
        this.state = state;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setValidFor(long duration, TimeUnit timeUnit) {
        HttpClientConnection httpClientConnection = this.managedConn;
        synchronized (httpClientConnection) {
            this.validDuration = duration;
            this.timeUnit = timeUnit;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void releaseConnection(boolean reusable) {
        if (this.released.compareAndSet(false, true)) {
            HttpClientConnection httpClientConnection = this.managedConn;
            synchronized (httpClientConnection) {
                if (reusable) {
                    this.manager.releaseConnection(this.managedConn, this.state, this.validDuration, this.timeUnit);
                } else {
                    try {
                        this.managedConn.close();
                        this.log.debug("Connection discarded");
                    }
                    catch (IOException ex) {
                        if (this.log.isDebugEnabled()) {
                            this.log.debug(ex.getMessage(), ex);
                        }
                    }
                    finally {
                        this.manager.releaseConnection(this.managedConn, null, 0L, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    @Override
    public void releaseConnection() {
        this.releaseConnection(this.reusable);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void abortConnection() {
        if (this.released.compareAndSet(false, true)) {
            HttpClientConnection httpClientConnection = this.managedConn;
            synchronized (httpClientConnection) {
                try {
                    this.managedConn.shutdown();
                    this.log.debug("Connection discarded");
                }
                catch (IOException ex) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug(ex.getMessage(), ex);
                    }
                }
                finally {
                    this.manager.releaseConnection(this.managedConn, null, 0L, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public boolean cancel() {
        boolean alreadyReleased = this.released.get();
        this.log.debug("Cancelling request execution");
        this.abortConnection();
        return !alreadyReleased;
    }

    public boolean isReleased() {
        return this.released.get();
    }

    @Override
    public void close() throws IOException {
        this.releaseConnection(false);
    }
}

