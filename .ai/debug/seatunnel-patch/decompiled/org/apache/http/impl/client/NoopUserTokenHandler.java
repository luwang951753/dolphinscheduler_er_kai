/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.client;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.protocol.HttpContext;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class NoopUserTokenHandler
implements UserTokenHandler {
    public static final NoopUserTokenHandler INSTANCE = new NoopUserTokenHandler();

    @Override
    public Object getUserToken(HttpContext context) {
        return null;
    }
}

