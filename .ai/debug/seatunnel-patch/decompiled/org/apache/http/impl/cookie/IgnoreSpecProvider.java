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
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.cookie.IgnoreSpec;
import org.apache.http.protocol.HttpContext;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class IgnoreSpecProvider
implements CookieSpecProvider {
    private volatile CookieSpec cookieSpec;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public CookieSpec create(HttpContext context) {
        if (this.cookieSpec == null) {
            IgnoreSpecProvider ignoreSpecProvider = this;
            synchronized (ignoreSpecProvider) {
                if (this.cookieSpec == null) {
                    this.cookieSpec = new IgnoreSpec();
                }
            }
        }
        return this.cookieSpec;
    }
}

