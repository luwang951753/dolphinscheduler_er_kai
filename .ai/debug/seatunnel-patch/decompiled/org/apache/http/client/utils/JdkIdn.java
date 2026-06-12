/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.client.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.utils.Idn;

@Deprecated
@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class JdkIdn
implements Idn {
    private final Method toUnicode;

    public JdkIdn() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.net.IDN");
        try {
            this.toUnicode = clazz.getMethod("toUnicode", String.class);
        }
        catch (SecurityException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public String toUnicode(String punycode) {
        try {
            return (String)this.toUnicode.invoke(null, punycode);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            throw new RuntimeException(t.getMessage(), t);
        }
    }
}

