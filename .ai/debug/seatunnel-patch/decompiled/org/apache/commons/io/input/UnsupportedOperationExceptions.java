/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.input;

class UnsupportedOperationExceptions {
    private static final String MARK_RESET = "mark/reset";

    UnsupportedOperationExceptions() {
    }

    static UnsupportedOperationException mark() {
        return UnsupportedOperationExceptions.method(MARK_RESET);
    }

    static UnsupportedOperationException method(String method) {
        return new UnsupportedOperationException(method + " not supported");
    }

    static UnsupportedOperationException reset() {
        return UnsupportedOperationExceptions.method(MARK_RESET);
    }
}

