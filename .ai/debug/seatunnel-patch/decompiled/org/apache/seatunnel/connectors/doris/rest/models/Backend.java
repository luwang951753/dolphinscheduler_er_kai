/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.apache.seatunnel.connectors.doris.rest.models.BackendRow;

@Deprecated
@JsonIgnoreProperties(ignoreUnknown=true)
public class Backend {
    @JsonProperty(value="rows")
    private List<BackendRow> rows;

    public List<BackendRow> getRows() {
        return this.rows;
    }

    public void setRows(List<BackendRow> rows) {
        this.rows = rows;
    }
}

