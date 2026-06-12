/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.annotation.Immutable;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLInitializationException;

@Immutable
public class SSLContexts {
    public static SSLContext createDefault() throws SSLInitializationException {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, null, null);
            return sslcontext;
        }
        catch (NoSuchAlgorithmException ex) {
            throw new SSLInitializationException(ex.getMessage(), ex);
        }
        catch (KeyManagementException ex) {
            throw new SSLInitializationException(ex.getMessage(), ex);
        }
    }

    public static SSLContext createSystemDefault() throws SSLInitializationException {
        try {
            return SSLContext.getDefault();
        }
        catch (NoSuchAlgorithmException ex) {
            return SSLContexts.createDefault();
        }
    }

    public static SSLContextBuilder custom() {
        return SSLContextBuilder.create();
    }
}

