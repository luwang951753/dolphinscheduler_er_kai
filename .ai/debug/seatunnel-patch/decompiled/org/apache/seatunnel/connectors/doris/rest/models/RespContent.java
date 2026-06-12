/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class RespContent {
    @JsonProperty(value="TxnId")
    private long txnId;
    @JsonProperty(value="Label")
    private String label;
    @JsonProperty(value="Status")
    private String status;
    @JsonProperty(value="TwoPhaseCommit")
    private String twoPhaseCommit;
    @JsonProperty(value="ExistingJobStatus")
    private String existingJobStatus;
    @JsonProperty(value="Message")
    private String message;
    @JsonProperty(value="ErrorURL")
    private String errorURL;

    public long getTxnId() {
        return this.txnId;
    }

    public String getLabel() {
        return this.label;
    }

    public String getStatus() {
        return this.status;
    }

    public String getTwoPhaseCommit() {
        return this.twoPhaseCommit;
    }

    public String getExistingJobStatus() {
        return this.existingJobStatus;
    }

    public String getMessage() {
        return this.message;
    }

    public String getErrorURL() {
        return this.errorURL;
    }

    @JsonProperty(value="TxnId")
    public void setTxnId(long txnId) {
        this.txnId = txnId;
    }

    @JsonProperty(value="Label")
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty(value="Status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty(value="TwoPhaseCommit")
    public void setTwoPhaseCommit(String twoPhaseCommit) {
        this.twoPhaseCommit = twoPhaseCommit;
    }

    @JsonProperty(value="ExistingJobStatus")
    public void setExistingJobStatus(String existingJobStatus) {
        this.existingJobStatus = existingJobStatus;
    }

    @JsonProperty(value="Message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty(value="ErrorURL")
    public void setErrorURL(String errorURL) {
        this.errorURL = errorURL;
    }

    public String toString() {
        return "RespContent(txnId=" + this.getTxnId() + ", label=" + this.getLabel() + ", status=" + this.getStatus() + ", twoPhaseCommit=" + this.getTwoPhaseCommit() + ", existingJobStatus=" + this.getExistingJobStatus() + ", message=" + this.getMessage() + ", errorURL=" + this.getErrorURL() + ")";
    }
}

