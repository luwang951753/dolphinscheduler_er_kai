/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest.models;

import java.util.Map;
import java.util.Objects;
import org.apache.seatunnel.connectors.doris.rest.models.Tablet;

public class QueryPlan {
    private int status;
    private String opaquedQueryPlan;
    private Map<String, Tablet> partitions;

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getOpaquedQueryPlan() {
        return this.opaquedQueryPlan;
    }

    public void setOpaquedQueryPlan(String opaquedQueryPlan) {
        this.opaquedQueryPlan = opaquedQueryPlan;
    }

    public Map<String, Tablet> getPartitions() {
        return this.partitions;
    }

    public void setPartitions(Map<String, Tablet> partitions) {
        this.partitions = partitions;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        QueryPlan queryPlan = (QueryPlan)o;
        return this.status == queryPlan.status && Objects.equals(this.opaquedQueryPlan, queryPlan.opaquedQueryPlan) && Objects.equals(this.partitions, queryPlan.partitions);
    }

    public int hashCode() {
        return Objects.hash(this.status, this.opaquedQueryPlan, this.partitions);
    }
}

