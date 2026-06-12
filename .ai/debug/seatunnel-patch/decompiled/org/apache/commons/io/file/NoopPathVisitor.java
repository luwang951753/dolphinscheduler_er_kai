/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.file;

import org.apache.commons.io.file.SimplePathVisitor;

public class NoopPathVisitor
extends SimplePathVisitor {
    public static final NoopPathVisitor INSTANCE = new NoopPathVisitor();
}

