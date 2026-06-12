/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.client;

import java.util.Collection;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.AuthenticationStrategyImpl;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class TargetAuthenticationStrategy
extends AuthenticationStrategyImpl {
    public static final TargetAuthenticationStrategy INSTANCE = new TargetAuthenticationStrategy();

    public TargetAuthenticationStrategy() {
        super(401, "WWW-Authenticate");
    }

    @Override
    Collection<String> getPreferredAuthSchemes(RequestConfig config) {
        return config.getTargetPreferredAuthSchemes();
    }
}

