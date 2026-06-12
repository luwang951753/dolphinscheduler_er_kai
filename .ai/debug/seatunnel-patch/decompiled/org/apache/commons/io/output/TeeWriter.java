/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.output;

import java.io.Writer;
import java.util.Collection;
import org.apache.commons.io.output.ProxyCollectionWriter;

public class TeeWriter
extends ProxyCollectionWriter {
    public TeeWriter(Collection<Writer> writers) {
        super(writers);
    }

    public TeeWriter(Writer ... writers) {
        super(writers);
    }
}

