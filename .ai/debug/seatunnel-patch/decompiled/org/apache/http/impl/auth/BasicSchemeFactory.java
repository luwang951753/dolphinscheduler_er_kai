/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.auth;

import java.nio.charset.Charset;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class BasicSchemeFactory
implements AuthSchemeFactory,
AuthSchemeProvider {
    private final Charset charset;

    public BasicSchemeFactory(Charset charset) {
        this.charset = charset;
    }

    public BasicSchemeFactory() {
        this(null);
    }

    @Override
    public AuthScheme newInstance(HttpParams params) {
        return new BasicScheme();
    }

    @Override
    public AuthScheme create(HttpContext context) {
        return new BasicScheme(this.charset);
    }
}

