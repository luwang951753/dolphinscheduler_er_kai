/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.input;

import org.apache.commons.io.input.CircularInputStream;

public class InfiniteCircularInputStream
extends CircularInputStream {
    public InfiniteCircularInputStream(byte[] repeatContent) {
        super(repeatContent, -1L);
    }
}

