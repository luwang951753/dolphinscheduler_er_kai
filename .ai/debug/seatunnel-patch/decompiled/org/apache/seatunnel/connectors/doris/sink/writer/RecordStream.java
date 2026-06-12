/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink.writer;

import java.io.IOException;
import java.io.InputStream;
import org.apache.seatunnel.connectors.doris.sink.writer.RecordBuffer;

public class RecordStream
extends InputStream {
    private final RecordBuffer recordBuffer;

    @Override
    public int read() throws IOException {
        return 0;
    }

    public RecordStream(int bufferSize, int bufferCount) {
        this.recordBuffer = new RecordBuffer(bufferSize, bufferCount);
    }

    public void startInput() {
        this.recordBuffer.startBufferData();
    }

    public void endInput() throws IOException {
        this.recordBuffer.stopBufferData();
    }

    @Override
    public int read(byte[] buff) throws IOException {
        try {
            return this.recordBuffer.read(buff);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(byte[] buff) throws IOException {
        try {
            this.recordBuffer.write(buff);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

