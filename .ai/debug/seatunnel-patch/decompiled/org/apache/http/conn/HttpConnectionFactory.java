/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.conn;

import org.apache.http.HttpConnection;
import org.apache.http.config.ConnectionConfig;

public interface HttpConnectionFactory<T, C extends HttpConnection> {
    public C create(T var1, ConnectionConfig var2);
}

