/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.client;

import org.apache.http.impl.client.Clock;

class SystemClock
implements Clock {
    SystemClock() {
    }

    @Override
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
}

