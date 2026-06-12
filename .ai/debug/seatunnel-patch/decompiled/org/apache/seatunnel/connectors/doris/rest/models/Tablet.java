/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest.models;

import java.util.List;
import java.util.Objects;

public class Tablet {
    private List<String> routings;
    private int version;
    private long versionHash;
    private long schemaHash;

    public List<String> getRoutings() {
        return this.routings;
    }

    public void setRoutings(List<String> routings) {
        this.routings = routings;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getVersionHash() {
        return this.versionHash;
    }

    public void setVersionHash(long versionHash) {
        this.versionHash = versionHash;
    }

    public long getSchemaHash() {
        return this.schemaHash;
    }

    public void setSchemaHash(long schemaHash) {
        this.schemaHash = schemaHash;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Tablet tablet = (Tablet)o;
        return this.version == tablet.version && this.versionHash == tablet.versionHash && this.schemaHash == tablet.schemaHash && Objects.equals(this.routings, tablet.routings);
    }

    public int hashCode() {
        return Objects.hash(this.routings, this.version, this.versionHash, this.schemaHash);
    }
}

