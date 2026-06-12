/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Deprecated
@JsonIgnoreProperties(ignoreUnknown=true)
public class BackendRow {
    @JsonProperty(value="HttpPort")
    private String httpPort;
    @JsonProperty(value="IP")
    private String ip;
    @JsonProperty(value="Alive")
    private Boolean alive;

    public String getHttpPort() {
        return this.httpPort;
    }

    public String getIp() {
        return this.ip;
    }

    public Boolean getAlive() {
        return this.alive;
    }

    @JsonProperty(value="HttpPort")
    public void setHttpPort(String httpPort) {
        this.httpPort = httpPort;
    }

    @JsonProperty(value="IP")
    public void setIp(String ip) {
        this.ip = ip;
    }

    @JsonProperty(value="Alive")
    public void setAlive(Boolean alive) {
        this.alive = alive;
    }

    public String toString() {
        return "BackendRow(httpPort=" + this.getHttpPort() + ", ip=" + this.getIp() + ", alive=" + this.getAlive() + ")";
    }
}

