/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.cookie;

import java.util.Collection;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class PublicSuffixFilter
implements CookieAttributeHandler {
    private final CookieAttributeHandler wrapped;
    private Collection<String> exceptions;
    private Collection<String> suffixes;
    private PublicSuffixMatcher matcher;

    public PublicSuffixFilter(CookieAttributeHandler wrapped) {
        this.wrapped = wrapped;
    }

    public void setPublicSuffixes(Collection<String> suffixes) {
        this.suffixes = suffixes;
        this.matcher = null;
    }

    public void setExceptions(Collection<String> exceptions) {
        this.exceptions = exceptions;
        this.matcher = null;
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin origin) {
        if (this.isForPublicSuffix(cookie)) {
            return false;
        }
        return this.wrapped.match(cookie, origin);
    }

    @Override
    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        this.wrapped.parse(cookie, value);
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
        this.wrapped.validate(cookie, origin);
    }

    private boolean isForPublicSuffix(Cookie cookie) {
        if (this.matcher == null) {
            this.matcher = new PublicSuffixMatcher(this.suffixes, this.exceptions);
        }
        return this.matcher.matches(cookie.getDomain());
    }
}

