/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.conn;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.CPoolEntry;
import org.apache.http.pool.AbstractConnPool;
import org.apache.http.pool.ConnFactory;
import org.apache.http.pool.PoolEntryCallback;

@Contract(threading=ThreadingBehavior.SAFE)
class CPool
extends AbstractConnPool<HttpRoute, ManagedHttpClientConnection, CPoolEntry> {
    private static final AtomicLong COUNTER = new AtomicLong();
    private final Log log = LogFactory.getLog(CPool.class);
    private final long timeToLive;
    private final TimeUnit timeUnit;

    public CPool(ConnFactory<HttpRoute, ManagedHttpClientConnection> connFactory, int defaultMaxPerRoute, int maxTotal, long timeToLive, TimeUnit timeUnit) {
        super(connFactory, defaultMaxPerRoute, maxTotal);
        this.timeToLive = timeToLive;
        this.timeUnit = timeUnit;
    }

    @Override
    protected CPoolEntry createEntry(HttpRoute route, ManagedHttpClientConnection conn) {
        String id = Long.toString(COUNTER.getAndIncrement());
        return new CPoolEntry(this.log, id, route, conn, this.timeToLive, this.timeUnit);
    }

    @Override
    protected boolean validate(CPoolEntry entry) {
        return !((ManagedHttpClientConnection)entry.getConnection()).isStale();
    }

    @Override
    protected void enumAvailable(PoolEntryCallback<HttpRoute, ManagedHttpClientConnection> callback) {
        super.enumAvailable(callback);
    }

    @Override
    protected void enumLeased(PoolEntryCallback<HttpRoute, ManagedHttpClientConnection> callback) {
        super.enumLeased(callback);
    }
}

