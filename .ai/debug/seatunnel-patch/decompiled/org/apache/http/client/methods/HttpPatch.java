/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.methods;

import java.net.URI;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpPatch
extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "PATCH";

    public HttpPatch() {
    }

    public HttpPatch(URI uri) {
        this.setURI(uri);
    }

    public HttpPatch(String uri) {
        this.setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}

