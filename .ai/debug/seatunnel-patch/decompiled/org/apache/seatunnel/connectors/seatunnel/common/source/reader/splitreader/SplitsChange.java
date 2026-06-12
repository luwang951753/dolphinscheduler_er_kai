/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader;

import java.util.Collection;
import java.util.Collections;

public abstract class SplitsChange<SplitT> {
    private final Collection<SplitT> splits;

    public Collection<SplitT> splits() {
        return Collections.unmodifiableCollection(this.splits);
    }

    public SplitsChange(Collection<SplitT> splits) {
        this.splits = splits;
    }
}

