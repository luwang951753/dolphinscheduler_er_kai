/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink.writer;

public class DorisSinkState {
    private final String labelPrefix;
    private final long checkpointId;

    public DorisSinkState(String labelPrefix, long checkpointId) {
        this.labelPrefix = labelPrefix;
        this.checkpointId = checkpointId;
    }

    public String getLabelPrefix() {
        return this.labelPrefix;
    }

    public long getCheckpointId() {
        return this.checkpointId;
    }

    public String toString() {
        return "DorisSinkState(labelPrefix=" + this.getLabelPrefix() + ", checkpointId=" + this.getCheckpointId() + ")";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DorisSinkState)) {
            return false;
        }
        DorisSinkState other = (DorisSinkState)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getCheckpointId() != other.getCheckpointId()) {
            return false;
        }
        String this$labelPrefix = this.getLabelPrefix();
        String other$labelPrefix = other.getLabelPrefix();
        return !(this$labelPrefix == null ? other$labelPrefix != null : !this$labelPrefix.equals(other$labelPrefix));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DorisSinkState;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $checkpointId = this.getCheckpointId();
        result = result * 59 + (int)($checkpointId >>> 32 ^ $checkpointId);
        String $labelPrefix = this.getLabelPrefix();
        result = result * 59 + ($labelPrefix == null ? 43 : $labelPrefix.hashCode());
        return result;
    }
}

