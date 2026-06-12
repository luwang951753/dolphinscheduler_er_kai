/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.conn.ssl;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.conn.ssl.AbstractVerifier;

@Deprecated
@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class AllowAllHostnameVerifier
extends AbstractVerifier {
    public static final AllowAllHostnameVerifier INSTANCE = new AllowAllHostnameVerifier();

    @Override
    public final void verify(String host, String[] cns, String[] subjectAlts) {
    }

    public final String toString() {
        return "ALLOW_ALL";
    }
}

