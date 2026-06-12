/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.params;

import java.net.InetAddress;
import java.util.Collection;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.params.HttpParams;

@Deprecated
public final class HttpClientParamConfig {
    private HttpClientParamConfig() {
    }

    public static RequestConfig getRequestConfig(HttpParams params) {
        return HttpClientParamConfig.getRequestConfig(params, RequestConfig.DEFAULT);
    }

    public static RequestConfig getRequestConfig(HttpParams params, RequestConfig defaultConfig) {
        String cookiePolicy;
        Collection proxySuthPrefs;
        Collection targetAuthPrefs;
        InetAddress localAddress;
        RequestConfig.Builder builder = RequestConfig.copy(defaultConfig).setSocketTimeout(params.getIntParameter("http.socket.timeout", defaultConfig.getSocketTimeout())).setStaleConnectionCheckEnabled(params.getBooleanParameter("http.connection.stalecheck", defaultConfig.isStaleConnectionCheckEnabled())).setConnectTimeout(params.getIntParameter("http.connection.timeout", defaultConfig.getConnectTimeout())).setExpectContinueEnabled(params.getBooleanParameter("http.protocol.expect-continue", defaultConfig.isExpectContinueEnabled())).setAuthenticationEnabled(params.getBooleanParameter("http.protocol.handle-authentication", defaultConfig.isAuthenticationEnabled())).setCircularRedirectsAllowed(params.getBooleanParameter("http.protocol.allow-circular-redirects", defaultConfig.isCircularRedirectsAllowed())).setConnectionRequestTimeout((int)params.getLongParameter("http.conn-manager.timeout", defaultConfig.getConnectionRequestTimeout())).setMaxRedirects(params.getIntParameter("http.protocol.max-redirects", defaultConfig.getMaxRedirects())).setRedirectsEnabled(params.getBooleanParameter("http.protocol.handle-redirects", defaultConfig.isRedirectsEnabled())).setRelativeRedirectsAllowed(!params.getBooleanParameter("http.protocol.reject-relative-redirect", !defaultConfig.isRelativeRedirectsAllowed()));
        HttpHost proxy = (HttpHost)params.getParameter("http.route.default-proxy");
        if (proxy != null) {
            builder.setProxy(proxy);
        }
        if ((localAddress = (InetAddress)params.getParameter("http.route.local-address")) != null) {
            builder.setLocalAddress(localAddress);
        }
        if ((targetAuthPrefs = (Collection)params.getParameter("http.auth.target-scheme-pref")) != null) {
            builder.setTargetPreferredAuthSchemes(targetAuthPrefs);
        }
        if ((proxySuthPrefs = (Collection)params.getParameter("http.auth.proxy-scheme-pref")) != null) {
            builder.setProxyPreferredAuthSchemes(proxySuthPrefs);
        }
        if ((cookiePolicy = (String)params.getParameter("http.protocol.cookie-policy")) != null) {
            builder.setCookieSpec(cookiePolicy);
        }
        return builder.build();
    }
}

