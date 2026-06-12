/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.cookie;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.cookie.IgnoreSpec;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

@Deprecated
@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class IgnoreSpecFactory
implements CookieSpecFactory,
CookieSpecProvider {
    @Override
    public CookieSpec newInstance(HttpParams params) {
        return new IgnoreSpec();
    }

    @Override
    public CookieSpec create(HttpContext context) {
        return new IgnoreSpec();
    }
}

