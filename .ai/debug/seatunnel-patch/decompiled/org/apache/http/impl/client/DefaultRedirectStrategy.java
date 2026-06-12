/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.Asserts;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class DefaultRedirectStrategy
implements RedirectStrategy {
    private final Log log = LogFactory.getLog(this.getClass());
    public static final int SC_PERMANENT_REDIRECT = 308;
    @Deprecated
    public static final String REDIRECT_LOCATIONS = "http.protocol.redirect-locations";
    public static final DefaultRedirectStrategy INSTANCE = new DefaultRedirectStrategy();
    private final String[] redirectMethods;

    public DefaultRedirectStrategy() {
        this(new String[]{"GET", "HEAD"});
    }

    public DefaultRedirectStrategy(String[] redirectMethods) {
        Object[] tmp = (String[])redirectMethods.clone();
        Arrays.sort(tmp);
        this.redirectMethods = tmp;
    }

    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        Args.notNull(request, "HTTP request");
        Args.notNull(response, "HTTP response");
        int statusCode = response.getStatusLine().getStatusCode();
        String method = request.getRequestLine().getMethod();
        Header locationHeader = response.getFirstHeader("location");
        switch (statusCode) {
            case 302: {
                return this.isRedirectable(method) && locationHeader != null;
            }
            case 301: 
            case 307: 
            case 308: {
                return this.isRedirectable(method);
            }
            case 303: {
                return true;
            }
        }
        return false;
    }

    public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        Args.notNull(request, "HTTP request");
        Args.notNull(response, "HTTP response");
        Args.notNull(context, "HTTP context");
        HttpClientContext clientContext = HttpClientContext.adapt(context);
        Header locationHeader = response.getFirstHeader("location");
        if (locationHeader == null) {
            throw new ProtocolException("Received redirect response " + response.getStatusLine() + " but no location header");
        }
        String location = locationHeader.getValue();
        if (this.log.isDebugEnabled()) {
            this.log.debug("Redirect requested to location '" + location + "'");
        }
        RequestConfig config = clientContext.getRequestConfig();
        URI uri = this.createLocationURI(location);
        try {
            if (config.isNormalizeUri()) {
                uri = URIUtils.normalizeSyntax(uri);
            }
            if (!uri.isAbsolute()) {
                if (!config.isRelativeRedirectsAllowed()) {
                    throw new ProtocolException("Relative redirect location '" + uri + "' not allowed");
                }
                HttpHost target = clientContext.getTargetHost();
                Asserts.notNull(target, "Target host");
                URI requestURI = new URI(request.getRequestLine().getUri());
                URI absoluteRequestURI = URIUtils.rewriteURI(requestURI, target, config.isNormalizeUri() ? URIUtils.NORMALIZE : URIUtils.NO_FLAGS);
                uri = URIUtils.resolve(absoluteRequestURI, uri);
            }
        }
        catch (URISyntaxException ex) {
            throw new ProtocolException(ex.getMessage(), ex);
        }
        RedirectLocations redirectLocations = (RedirectLocations)clientContext.getAttribute(REDIRECT_LOCATIONS);
        if (redirectLocations == null) {
            redirectLocations = new RedirectLocations();
            context.setAttribute(REDIRECT_LOCATIONS, redirectLocations);
        }
        if (!config.isCircularRedirectsAllowed() && redirectLocations.contains(uri)) {
            throw new CircularRedirectException("Circular redirect to '" + uri + "'");
        }
        redirectLocations.add(uri);
        return uri;
    }

    protected URI createLocationURI(String location) throws ProtocolException {
        try {
            return new URI(location);
        }
        catch (URISyntaxException ex) {
            throw new ProtocolException("Invalid redirect URI: " + location, ex);
        }
    }

    protected boolean isRedirectable(String method) {
        return Arrays.binarySearch(this.redirectMethods, method) >= 0;
    }

    @Override
    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        URI uri = this.getLocationURI(request, response, context);
        String method = request.getRequestLine().getMethod();
        if (method.equalsIgnoreCase("HEAD")) {
            return new HttpHead(uri);
        }
        if (method.equalsIgnoreCase("GET")) {
            return new HttpGet(uri);
        }
        int status = response.getStatusLine().getStatusCode();
        return status == 307 || status == 308 ? RequestBuilder.copy(request).setUri(uri).build() : new HttpGet(uri);
    }
}

