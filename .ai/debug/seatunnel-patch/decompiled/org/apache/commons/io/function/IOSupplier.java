/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.function;

import java.io.IOException;

@FunctionalInterface
public interface IOSupplier<T> {
    public T get() throws IOException;
}

