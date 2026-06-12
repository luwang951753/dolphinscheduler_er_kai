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
import org.apache.http.impl.cookie.DefaultCookieSpec;

@Deprecated
@Contract(threading=ThreadingBehavior.SAFE)
public class BestMatchSpec
extends DefaultCookieSpec {
    public BestMatchSpec(String[] datepatterns, boolean oneHeader) {
        super(datepatterns, oneHeader);
    }

    public BestMatchSpec() {
        this(null, false);
    }

    @Override
    public String toString() {
        return "best-match";
    }
}

