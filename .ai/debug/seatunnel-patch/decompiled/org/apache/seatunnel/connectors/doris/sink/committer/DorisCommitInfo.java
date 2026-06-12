/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink.committer;

import java.io.Serializable;

public class DorisCommitInfo
implements Serializable {
    private final String hostPort;
    private final String db;
    private final long txbID;

    public DorisCommitInfo(String hostPort, String db, long txbID) {
        this.hostPort = hostPort;
        this.db = db;
        this.txbID = txbID;
    }

    public String getHostPort() {
        return this.hostPort;
    }

    public String getDb() {
        return this.db;
    }

    public long getTxbID() {
        return this.txbID;
    }

    public String toString() {
        return "DorisCommitInfo(hostPort=" + this.getHostPort() + ", db=" + this.getDb() + ", txbID=" + this.getTxbID() + ")";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DorisCommitInfo)) {
            return false;
        }
        DorisCommitInfo other = (DorisCommitInfo)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getTxbID() != other.getTxbID()) {
            return false;
        }
        String this$hostPort = this.getHostPort();
        String other$hostPort = other.getHostPort();
        if (this$hostPort == null ? other$hostPort != null : !this$hostPort.equals(other$hostPort)) {
            return false;
        }
        String this$db = this.getDb();
        String other$db = other.getDb();
        return !(this$db == null ? other$db != null : !this$db.equals(other$db));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DorisCommitInfo;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $txbID = this.getTxbID();
        result = result * 59 + (int)($txbID >>> 32 ^ $txbID);
        String $hostPort = this.getHostPort();
        result = result * 59 + ($hostPort == null ? 43 : $hostPort.hashCode());
        String $db = this.getDb();
        result = result * 59 + ($db == null ? 43 : $db.hashCode());
        return result;
    }
}

