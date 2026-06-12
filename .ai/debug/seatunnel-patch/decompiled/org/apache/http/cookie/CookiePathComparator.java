/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.cookie;

import java.io.Serializable;
import java.util.Comparator;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.cookie.Cookie;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class CookiePathComparator
implements Serializable,
Comparator<Cookie> {
    public static final CookiePathComparator INSTANCE = new CookiePathComparator();
    private static final long serialVersionUID = 7523645369616405818L;

    private String normalizePath(Cookie cookie) {
        String path = cookie.getPath();
        if (path == null) {
            path = "/";
        }
        if (!path.endsWith("/")) {
            path = path + '/';
        }
        return path;
    }

    @Override
    public int compare(Cookie c1, Cookie c2) {
        String path2;
        String path1 = this.normalizePath(c1);
        if (path1.equals(path2 = this.normalizePath(c2))) {
            return 0;
        }
        if (path1.startsWith(path2)) {
            return -1;
        }
        if (path2.startsWith(path1)) {
            return 1;
        }
        return 0;
    }
}

