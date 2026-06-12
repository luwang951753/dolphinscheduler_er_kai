/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PartitionDefinition
implements Serializable,
Comparable<PartitionDefinition> {
    private final String database;
    private final String table;
    private final String beAddress;
    private final Set<Long> tabletIds;
    private final String queryPlan;

    public PartitionDefinition(String database, String table, String beAddress, Set<Long> tabletIds, String queryPlan) throws IllegalArgumentException {
        this.database = database;
        this.table = table;
        this.beAddress = beAddress;
        this.tabletIds = tabletIds;
        this.queryPlan = queryPlan;
    }

    public String getBeAddress() {
        return this.beAddress;
    }

    public Set<Long> getTabletIds() {
        return this.tabletIds;
    }

    public String getDatabase() {
        return this.database;
    }

    public String getTable() {
        return this.table;
    }

    public String getQueryPlan() {
        return this.queryPlan;
    }

    @Override
    public int compareTo(PartitionDefinition o) {
        int cmp = this.database.compareTo(o.database);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.table.compareTo(o.table);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.beAddress.compareTo(o.beAddress);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.queryPlan.compareTo(o.queryPlan);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.tabletIds.size() - o.tabletIds.size();
        if (cmp != 0) {
            return cmp;
        }
        HashSet<Long> similar = new HashSet<Long>(this.tabletIds);
        HashSet<Long> diffSelf = new HashSet<Long>(this.tabletIds);
        HashSet<Long> diffOther = new HashSet<Long>(o.tabletIds);
        similar.retainAll(o.tabletIds);
        diffSelf.removeAll(similar);
        diffOther.removeAll(similar);
        if (diffSelf.size() == 0) {
            return 0;
        }
        long diff = Collections.min(diffSelf) - Collections.min(diffOther);
        return diff < 0L ? -1 : 1;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        PartitionDefinition that = (PartitionDefinition)o;
        return Objects.equals(this.database, that.database) && Objects.equals(this.table, that.table) && Objects.equals(this.beAddress, that.beAddress) && Objects.equals(this.tabletIds, that.tabletIds) && Objects.equals(this.queryPlan, that.queryPlan);
    }

    public int hashCode() {
        int result = this.database.hashCode();
        result = 31 * result + this.table.hashCode();
        result = 31 * result + this.beAddress.hashCode();
        result = 31 * result + this.queryPlan.hashCode();
        result = 31 * result + this.tabletIds.hashCode();
        return result;
    }

    public String toString() {
        return "PartitionDefinition{, database='" + this.database + '\'' + ", table='" + this.table + '\'' + ", beAddress='" + this.beAddress + '\'' + ", tabletIds=" + this.tabletIds + ", queryPlan='" + this.queryPlan + '\'' + '}';
    }
}

