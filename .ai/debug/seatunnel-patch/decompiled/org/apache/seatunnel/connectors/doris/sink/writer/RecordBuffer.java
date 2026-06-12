/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink.writer;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordBuffer {
    private static final Logger log = LoggerFactory.getLogger(RecordBuffer.class);
    BlockingQueue<ByteBuffer> writeQueue;
    BlockingQueue<ByteBuffer> readQueue;
    int bufferCapacity;
    int queueSize;
    ByteBuffer currentWriteBuffer;
    ByteBuffer currentReadBuffer;

    public RecordBuffer(int capacity, int queueSize) {
        log.info("init RecordBuffer capacity {}, count {}", (Object)capacity, (Object)queueSize);
        Preconditions.checkState(capacity > 0);
        Preconditions.checkState(queueSize > 1);
        this.writeQueue = new ArrayBlockingQueue<ByteBuffer>(queueSize);
        for (int index = 0; index < queueSize; ++index) {
            this.writeQueue.add(ByteBuffer.allocate(capacity));
        }
        this.readQueue = new LinkedBlockingDeque<ByteBuffer>();
        this.bufferCapacity = capacity;
        this.queueSize = queueSize;
    }

    public void startBufferData() {
        log.info("start buffer data, read queue size {}, write queue size {}", (Object)this.readQueue.size(), (Object)this.writeQueue.size());
        Preconditions.checkState(this.readQueue.size() == 0);
        Preconditions.checkState(this.writeQueue.size() == this.queueSize);
        for (ByteBuffer byteBuffer : this.writeQueue) {
            Preconditions.checkState(byteBuffer.position() == 0);
            Preconditions.checkState(byteBuffer.remaining() == this.bufferCapacity);
        }
    }

    public void stopBufferData() throws IOException {
        try {
            boolean isEmpty = false;
            if (this.currentWriteBuffer != null) {
                this.currentWriteBuffer.flip();
                isEmpty = this.currentWriteBuffer.limit() == 0;
                this.readQueue.put(this.currentWriteBuffer);
                this.currentWriteBuffer = null;
            }
            if (!isEmpty) {
                ByteBuffer byteBuffer = this.writeQueue.take();
                byteBuffer.flip();
                Preconditions.checkState(byteBuffer.limit() == 0);
                this.readQueue.put(byteBuffer);
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void write(byte[] buf) throws InterruptedException {
        int wPos = 0;
        do {
            if (this.currentWriteBuffer == null) {
                this.currentWriteBuffer = this.writeQueue.take();
            }
            int available = this.currentWriteBuffer.remaining();
            int nWrite = Math.min(available, buf.length - wPos);
            this.currentWriteBuffer.put(buf, wPos, nWrite);
            wPos += nWrite;
            if (this.currentWriteBuffer.remaining() != 0) continue;
            this.currentWriteBuffer.flip();
            this.readQueue.put(this.currentWriteBuffer);
            this.currentWriteBuffer = null;
        } while (wPos != buf.length);
    }

    public int read(byte[] buf) throws InterruptedException {
        if (this.currentReadBuffer == null) {
            this.currentReadBuffer = this.readQueue.take();
        }
        if (this.currentReadBuffer.limit() == 0) {
            this.recycleBuffer(this.currentReadBuffer);
            this.currentReadBuffer = null;
            Preconditions.checkState(this.readQueue.size() == 0);
            return -1;
        }
        int available = this.currentReadBuffer.remaining();
        int nRead = Math.min(available, buf.length);
        this.currentReadBuffer.get(buf, 0, nRead);
        if (this.currentReadBuffer.remaining() == 0) {
            this.recycleBuffer(this.currentReadBuffer);
            this.currentReadBuffer = null;
        }
        return nRead;
    }

    private void recycleBuffer(ByteBuffer buffer) throws InterruptedException {
        buffer.clear();
        this.writeQueue.put(buffer);
    }

    public int getWriteQueueSize() {
        return this.writeQueue.size();
    }

    public int getReadQueueSize() {
        return this.readQueue.size();
    }
}

