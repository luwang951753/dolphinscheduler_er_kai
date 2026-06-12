/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.input;

import java.io.InputStream;
import org.apache.commons.io.input.ClosedInputStream;
import org.apache.commons.io.input.ProxyInputStream;

public class CloseShieldInputStream
extends ProxyInputStream {
    public static CloseShieldInputStream wrap(InputStream inputStream) {
        return new CloseShieldInputStream(inputStream);
    }

    @Deprecated
    public CloseShieldInputStream(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    public void close() {
        this.in = ClosedInputStream.CLOSED_INPUT_STREAM;
    }
}

