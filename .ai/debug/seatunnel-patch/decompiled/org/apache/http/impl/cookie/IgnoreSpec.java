/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.cookie;

import java.util.Collections;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.cookie.CookieSpecBase;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class IgnoreSpec
extends CookieSpecBase {
    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public List<Cookie> parse(Header header, CookieOrigin origin) throws MalformedCookieException {
        return Collections.emptyList();
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin origin) {
        return false;
    }

    @Override
    public List<Header> formatCookies(List<Cookie> cookies) {
        return Collections.emptyList();
    }

    @Override
    public Header getVersionHeader() {
        return null;
    }
}

