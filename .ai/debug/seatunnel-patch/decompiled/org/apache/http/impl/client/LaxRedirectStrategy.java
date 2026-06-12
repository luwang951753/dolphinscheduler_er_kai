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
import org.apache.http.impl.client.DefaultRedirectStrategy;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class LaxRedirectStrategy
extends DefaultRedirectStrategy {
    public static final LaxRedirectStrategy INSTANCE = new LaxRedirectStrategy();

    public LaxRedirectStrategy() {
        super(new String[]{"GET", "POST", "HEAD", "DELETE"});
    }
}

