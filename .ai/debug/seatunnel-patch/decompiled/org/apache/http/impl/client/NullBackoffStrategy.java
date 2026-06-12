/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.client;

import org.apache.http.HttpResponse;
import org.apache.http.client.ConnectionBackoffStrategy;

public class NullBackoffStrategy
implements ConnectionBackoffStrategy {
    @Override
    public boolean shouldBackoff(Throwable t) {
        return false;
    }

    @Override
    public boolean shouldBackoff(HttpResponse resp) {
        return false;
    }
}

