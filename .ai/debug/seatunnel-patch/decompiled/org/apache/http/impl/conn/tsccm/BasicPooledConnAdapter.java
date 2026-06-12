/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.conn.tsccm;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.AbstractPoolEntry;
import org.apache.http.impl.conn.AbstractPooledConnAdapter;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

@Deprecated
public class BasicPooledConnAdapter
extends AbstractPooledConnAdapter {
    protected BasicPooledConnAdapter(ThreadSafeClientConnManager tsccm, AbstractPoolEntry entry) {
        super((ClientConnectionManager)tsccm, entry);
        this.markReusable();
    }

    @Override
    protected ClientConnectionManager getManager() {
        return super.getManager();
    }

    @Override
    protected AbstractPoolEntry getPoolEntry() {
        return super.getPoolEntry();
    }

    @Override
    protected void detach() {
        super.detach();
    }
}

