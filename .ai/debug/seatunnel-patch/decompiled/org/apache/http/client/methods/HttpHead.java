/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.methods;

import java.net.URI;
import org.apache.http.client.methods.HttpRequestBase;

public class HttpHead
extends HttpRequestBase {
    public static final String METHOD_NAME = "HEAD";

    public HttpHead() {
    }

    public HttpHead(URI uri) {
        this.setURI(uri);
    }

    public HttpHead(String uri) {
        this.setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}

