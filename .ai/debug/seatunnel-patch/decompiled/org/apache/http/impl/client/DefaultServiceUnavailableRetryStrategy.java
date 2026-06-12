/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.client;

import org.apache.http.HttpResponse;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class DefaultServiceUnavailableRetryStrategy
implements ServiceUnavailableRetryStrategy {
    private final int maxRetries;
    private final long retryInterval;

    public DefaultServiceUnavailableRetryStrategy(int maxRetries, int retryInterval) {
        Args.positive(maxRetries, "Max retries");
        Args.positive(retryInterval, "Retry interval");
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    public DefaultServiceUnavailableRetryStrategy() {
        this(1, 1000);
    }

    @Override
    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        return executionCount <= this.maxRetries && response.getStatusLine().getStatusCode() == 503;
    }

    @Override
    public long getRetryInterval() {
        return this.retryInterval;
    }
}

