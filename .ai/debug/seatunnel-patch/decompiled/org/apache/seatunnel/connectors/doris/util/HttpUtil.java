/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.util;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

public class HttpUtil {
    private final HttpClientBuilder httpClientBuilder = HttpClients.custom().setRedirectStrategy(new DefaultRedirectStrategy(){

        @Override
        protected boolean isRedirectable(String method) {
            return true;
        }
    });

    public CloseableHttpClient getHttpClient() {
        return this.httpClientBuilder.build();
    }
}

