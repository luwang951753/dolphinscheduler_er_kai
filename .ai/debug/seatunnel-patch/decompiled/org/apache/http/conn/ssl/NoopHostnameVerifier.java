/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.conn.ssl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class NoopHostnameVerifier
implements HostnameVerifier {
    public static final NoopHostnameVerifier INSTANCE = new NoopHostnameVerifier();

    @Override
    public boolean verify(String s, SSLSession sslSession) {
        return true;
    }

    public final String toString() {
        return "NO_OP";
    }
}

