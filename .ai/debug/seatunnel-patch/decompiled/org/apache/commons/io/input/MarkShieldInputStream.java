/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.input;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.io.input.UnsupportedOperationExceptions;

public class MarkShieldInputStream
extends ProxyInputStream {
    public MarkShieldInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void mark(int readlimit) {
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void reset() throws IOException {
        throw UnsupportedOperationExceptions.reset();
    }
}

