/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.entity.LazyDecompressingInputStream;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.util.Args;

public class DecompressingEntity
extends HttpEntityWrapper {
    private static final int BUFFER_SIZE = 2048;
    private final InputStreamFactory inputStreamFactory;
    private InputStream content;

    public DecompressingEntity(HttpEntity wrapped, InputStreamFactory inputStreamFactory) {
        super(wrapped);
        this.inputStreamFactory = inputStreamFactory;
    }

    private InputStream getDecompressingStream() throws IOException {
        InputStream in = this.wrappedEntity.getContent();
        return new LazyDecompressingInputStream(in, this.inputStreamFactory);
    }

    @Override
    public InputStream getContent() throws IOException {
        if (this.wrappedEntity.isStreaming()) {
            if (this.content == null) {
                this.content = this.getDecompressingStream();
            }
            return this.content;
        }
        return this.getDecompressingStream();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        Args.notNull(outStream, "Output stream");
        InputStream inStream = this.getContent();
        try {
            int l;
            byte[] buffer = new byte[2048];
            while ((l = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, l);
            }
        }
        finally {
            inStream.close();
        }
    }

    @Override
    public Header getContentEncoding() {
        return null;
    }

    @Override
    public long getContentLength() {
        return -1L;
    }
}

