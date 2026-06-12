/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink.writer;

public class LabelGenerator {
    private String labelPrefix;
    private boolean enable2PC;

    public LabelGenerator(String labelPrefix, boolean enable2PC) {
        this.labelPrefix = labelPrefix;
        this.enable2PC = enable2PC;
    }

    public String generateLabel(long chkId) {
        return this.enable2PC ? this.labelPrefix + "_" + chkId : this.labelPrefix + "_" + System.currentTimeMillis();
    }
}

