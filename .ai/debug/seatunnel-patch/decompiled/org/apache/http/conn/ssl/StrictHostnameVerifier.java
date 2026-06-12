/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.conn.ssl;

import javax.net.ssl.SSLException;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.conn.ssl.AbstractVerifier;

@Deprecated
@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class StrictHostnameVerifier
extends AbstractVerifier {
    public static final StrictHostnameVerifier INSTANCE = new StrictHostnameVerifier();

    @Override
    public final void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
        this.verify(host, cns, subjectAlts, true);
    }

    public final String toString() {
        return "STRICT";
    }
}

