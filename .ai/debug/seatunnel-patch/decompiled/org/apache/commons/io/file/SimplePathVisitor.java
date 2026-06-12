/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.file;

import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import org.apache.commons.io.file.PathVisitor;

public abstract class SimplePathVisitor
extends SimpleFileVisitor<Path>
implements PathVisitor {
    protected SimplePathVisitor() {
    }
}

