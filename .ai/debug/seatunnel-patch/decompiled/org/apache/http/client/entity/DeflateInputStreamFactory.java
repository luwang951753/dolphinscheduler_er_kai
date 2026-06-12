/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.client.entity;

import java.io.IOException;
import java.io.InputStream;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.entity.DeflateInputStream;
import org.apache.http.client.entity.InputStreamFactory;

@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class DeflateInputStreamFactory
implements InputStreamFactory {
    private static final DeflateInputStreamFactory INSTANCE = new DeflateInputStreamFactory();

    public static DeflateInputStreamFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public InputStream create(InputStream inputStream) throws IOException {
        return new DeflateInputStream(inputStream);
    }
}

