/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class BackendV2 {
    @JsonProperty(value="backends")
    private List<BackendRowV2> backends;

    public List<BackendRowV2> getBackends() {
        return this.backends;
    }

    public void setBackends(List<BackendRowV2> backends) {
        this.backends = backends;
    }

    public static class BackendRowV2 {
        @JsonProperty(value="ip")
        public String ip;
        @JsonProperty(value="http_port")
        public int httpPort;
        @JsonProperty(value="is_alive")
        public boolean isAlive;

        public String getIp() {
            return this.ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getHttpPort() {
            return this.httpPort;
        }

        public void setHttpPort(int httpPort) {
            this.httpPort = httpPort;
        }

        public boolean isAlive() {
            return this.isAlive;
        }

        public void setAlive(boolean alive) {
            this.isAlive = alive;
        }

        public String toBackendString() {
            return this.ip + ":" + this.httpPort;
        }
    }
}

