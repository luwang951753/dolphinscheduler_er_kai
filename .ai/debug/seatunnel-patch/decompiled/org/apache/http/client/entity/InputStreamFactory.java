/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.entity;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamFactory {
    public InputStream create(InputStream var1) throws IOException;
}

