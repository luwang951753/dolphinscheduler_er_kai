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
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public abstract class AbstractCookieAttributeHandler
implements CookieAttributeHandler {
    @Override
    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin origin) {
        return true;
    }
}

