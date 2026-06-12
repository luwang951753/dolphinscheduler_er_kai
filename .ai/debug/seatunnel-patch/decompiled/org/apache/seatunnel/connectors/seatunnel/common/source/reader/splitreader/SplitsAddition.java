/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader;

import java.util.Collection;
import org.apache.seatunnel.connectors.seatunnel.common.source.reader.splitreader.SplitsChange;

public class SplitsAddition<SplitT>
extends SplitsChange<SplitT> {
    public SplitsAddition(Collection<SplitT> splits) {
        super(splits);
    }

    public String toString() {
        return String.format("SplitAddition:[%s]", this.splits());
    }
}

