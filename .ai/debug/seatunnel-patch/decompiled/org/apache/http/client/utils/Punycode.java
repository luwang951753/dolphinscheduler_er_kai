/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.client.utils;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.utils.Idn;
import org.apache.http.client.utils.JdkIdn;
import org.apache.http.client.utils.Rfc3492Idn;

@Deprecated
@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class Punycode {
    private static final Idn impl;

    public static String toUnicode(String punycode) {
        return impl.toUnicode(punycode);
    }

    static {
        Idn _impl;
        try {
            _impl = new JdkIdn();
        }
        catch (Exception e) {
            _impl = new Rfc3492Idn();
        }
        impl = _impl;
    }
}

