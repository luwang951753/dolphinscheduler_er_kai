/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.methods;

import java.net.URI;
import org.apache.http.client.methods.HttpRequestBase;

public class HttpGet
extends HttpRequestBase {
    public static final String METHOD_NAME = "GET";

    public HttpGet() {
    }

    public HttpGet(URI uri) {
        this.setURI(uri);
    }

    public HttpGet(String uri) {
        this.setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}

