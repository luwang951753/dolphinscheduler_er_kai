/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.routing.RouteInfo;
import org.apache.http.util.Args;
import org.apache.http.util.TextUtils;

public class URIUtils {
    public static final EnumSet<UriFlag> NO_FLAGS = EnumSet.noneOf(UriFlag.class);
    public static final EnumSet<UriFlag> DROP_FRAGMENT = EnumSet.of(UriFlag.DROP_FRAGMENT);
    public static final EnumSet<UriFlag> NORMALIZE = EnumSet.of(UriFlag.NORMALIZE);
    public static final EnumSet<UriFlag> DROP_FRAGMENT_AND_NORMALIZE = EnumSet.of(UriFlag.DROP_FRAGMENT, UriFlag.NORMALIZE);

    @Deprecated
    public static URI createURI(String scheme, String host, int port, String path, String query, String fragment) throws URISyntaxException {
        StringBuilder buffer = new StringBuilder();
        if (host != null) {
            if (scheme != null) {
                buffer.append(scheme);
                buffer.append("://");
            }
            buffer.append(host);
            if (port > 0) {
                buffer.append(':');
                buffer.append(port);
            }
        }
        if (path == null || !path.startsWith("/")) {
            buffer.append('/');
        }
        if (path != null) {
            buffer.append(path);
        }
        if (query != null) {
            buffer.append('?');
            buffer.append(query);
        }
        if (fragment != null) {
            buffer.append('#');
            buffer.append(fragment);
        }
        return new URI(buffer.toString());
    }

    @Deprecated
    public static URI rewriteURI(URI uri, HttpHost target, boolean dropFragment) throws URISyntaxException {
        return URIUtils.rewriteURI(uri, target, dropFragment ? DROP_FRAGMENT : NO_FLAGS);
    }

    public static URI rewriteURI(URI uri, HttpHost target, EnumSet<UriFlag> flags) throws URISyntaxException {
        Args.notNull(uri, "URI");
        Args.notNull(flags, "URI flags");
        if (uri.isOpaque()) {
            return uri;
        }
        URIBuilder uribuilder = new URIBuilder(uri);
        if (target != null) {
            uribuilder.setScheme(target.getSchemeName());
            uribuilder.setHost(target.getHostName());
            uribuilder.setPort(target.getPort());
        } else {
            uribuilder.setScheme(null);
            uribuilder.setHost(null);
            uribuilder.setPort(-1);
        }
        if (flags.contains((Object)UriFlag.DROP_FRAGMENT)) {
            uribuilder.setFragment(null);
        }
        if (flags.contains((Object)UriFlag.NORMALIZE)) {
            List<String> originalPathSegments = uribuilder.getPathSegments();
            ArrayList<String> pathSegments = new ArrayList<String>(originalPathSegments);
            Iterator it = pathSegments.iterator();
            while (it.hasNext()) {
                String pathSegment = (String)it.next();
                if (!pathSegment.isEmpty() || !it.hasNext()) continue;
                it.remove();
            }
            if (pathSegments.size() != originalPathSegments.size()) {
                uribuilder.setPathSegments(pathSegments);
            }
        }
        if (uribuilder.isPathEmpty()) {
            uribuilder.setPathSegments("");
        }
        return uribuilder.build();
    }

    public static URI rewriteURI(URI uri, HttpHost target) throws URISyntaxException {
        return URIUtils.rewriteURI(uri, target, NORMALIZE);
    }

    public static URI rewriteURI(URI uri) throws URISyntaxException {
        Args.notNull(uri, "URI");
        if (uri.isOpaque()) {
            return uri;
        }
        URIBuilder uribuilder = new URIBuilder(uri);
        if (uribuilder.getUserInfo() != null) {
            uribuilder.setUserInfo(null);
        }
        if (uribuilder.getPathSegments().isEmpty()) {
            uribuilder.setPathSegments("");
        }
        if (TextUtils.isEmpty(uribuilder.getPath())) {
            uribuilder.setPath("/");
        }
        if (uribuilder.getHost() != null) {
            uribuilder.setHost(uribuilder.getHost().toLowerCase(Locale.ROOT));
        }
        uribuilder.setFragment(null);
        return uribuilder.build();
    }

    public static URI rewriteURIForRoute(URI uri, RouteInfo route) throws URISyntaxException {
        return URIUtils.rewriteURIForRoute(uri, route, true);
    }

    public static URI rewriteURIForRoute(URI uri, RouteInfo route, boolean normalizeUri) throws URISyntaxException {
        if (uri == null) {
            return null;
        }
        if (route.getProxyHost() != null && !route.isTunnelled()) {
            return uri.isAbsolute() ? URIUtils.rewriteURI(uri) : URIUtils.rewriteURI(uri, route.getTargetHost(), normalizeUri ? DROP_FRAGMENT_AND_NORMALIZE : DROP_FRAGMENT);
        }
        return uri.isAbsolute() ? URIUtils.rewriteURI(uri, null, normalizeUri ? DROP_FRAGMENT_AND_NORMALIZE : DROP_FRAGMENT) : URIUtils.rewriteURI(uri);
    }

    public static URI resolve(URI baseURI, String reference) {
        return URIUtils.resolve(baseURI, URI.create(reference));
    }

    public static URI resolve(URI baseURI, URI reference) {
        URI resolved;
        Args.notNull(baseURI, "Base URI");
        Args.notNull(reference, "Reference URI");
        String s = reference.toASCIIString();
        if (s.startsWith("?")) {
            String baseUri = baseURI.toASCIIString();
            int i = baseUri.indexOf(63);
            baseUri = i > -1 ? baseUri.substring(0, i) : baseUri;
            return URI.create(baseUri + s);
        }
        boolean emptyReference = s.isEmpty();
        if (emptyReference) {
            resolved = baseURI.resolve(URI.create("#"));
            String resolvedString = resolved.toASCIIString();
            resolved = URI.create(resolvedString.substring(0, resolvedString.indexOf(35)));
        } else {
            resolved = baseURI.resolve(reference);
        }
        try {
            return URIUtils.normalizeSyntax(resolved);
        }
        catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static URI normalizeSyntax(URI uri) throws URISyntaxException {
        if (uri.isOpaque() || uri.getAuthority() == null) {
            return uri;
        }
        URIBuilder builder = new URIBuilder(uri);
        List<String> inputSegments = builder.getPathSegments();
        Stack<String> outputSegments = new Stack<String>();
        for (String inputSegment : inputSegments) {
            if (".".equals(inputSegment)) continue;
            if ("..".equals(inputSegment)) {
                if (outputSegments.isEmpty()) continue;
                outputSegments.pop();
                continue;
            }
            outputSegments.push(inputSegment);
        }
        if (outputSegments.size() == 0) {
            outputSegments.add("");
        }
        builder.setPathSegments(outputSegments);
        if (builder.getScheme() != null) {
            builder.setScheme(builder.getScheme().toLowerCase(Locale.ROOT));
        }
        if (builder.getHost() != null) {
            builder.setHost(builder.getHost().toLowerCase(Locale.ROOT));
        }
        return builder.build();
    }

    public static HttpHost extractHost(URI uri) {
        if (uri == null) {
            return null;
        }
        if (uri.isAbsolute()) {
            if (uri.getHost() == null) {
                if (uri.getAuthority() != null) {
                    int port;
                    String hostname;
                    String content = uri.getAuthority();
                    int at = content.indexOf(64);
                    if (at != -1) {
                        content = content.substring(at + 1);
                    }
                    String scheme = uri.getScheme();
                    at = content.indexOf(":");
                    if (at != -1) {
                        hostname = content.substring(0, at);
                        try {
                            String portText = content.substring(at + 1);
                            port = !TextUtils.isEmpty(portText) ? Integer.parseInt(portText) : -1;
                        }
                        catch (NumberFormatException ex) {
                            return null;
                        }
                    } else {
                        hostname = content;
                        port = -1;
                    }
                    try {
                        return new HttpHost(hostname, port, scheme);
                    }
                    catch (IllegalArgumentException ex) {
                        return null;
                    }
                }
            } else {
                return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            }
        }
        return null;
    }

    public static URI resolve(URI originalURI, HttpHost target, List<URI> redirects) throws URISyntaxException {
        URIBuilder uribuilder;
        Args.notNull(originalURI, "Request URI");
        if (redirects == null || redirects.isEmpty()) {
            uribuilder = new URIBuilder(originalURI);
        } else {
            uribuilder = new URIBuilder(redirects.get(redirects.size() - 1));
            String frag = uribuilder.getFragment();
            for (int i = redirects.size() - 1; frag == null && i >= 0; --i) {
                frag = redirects.get(i).getFragment();
            }
            uribuilder.setFragment(frag);
        }
        if (uribuilder.getFragment() == null) {
            uribuilder.setFragment(originalURI.getFragment());
        }
        if (target != null && !uribuilder.isAbsolute()) {
            uribuilder.setScheme(target.getSchemeName());
            uribuilder.setHost(target.getHostName());
            uribuilder.setPort(target.getPort());
        }
        return uribuilder.build();
    }

    private URIUtils() {
    }

    public static enum UriFlag {
        DROP_FRAGMENT,
        NORMALIZE;

    }
}

