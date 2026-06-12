/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.cookie;

import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.impl.cookie.RFC6265CookieSpec;

class RFC6265CookieSpecBase
extends RFC6265CookieSpec {
    RFC6265CookieSpecBase(CommonCookieAttributeHandler ... handlers) {
        super(handlers);
    }
}

