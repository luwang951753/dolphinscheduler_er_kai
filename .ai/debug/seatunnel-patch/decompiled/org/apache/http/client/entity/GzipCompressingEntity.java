/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.Args;

public class GzipCompressingEntity
extends HttpEntityWrapper {
    private static final String GZIP_CODEC = "gzip";

    public GzipCompressingEntity(HttpEntity entity) {
        super(entity);
    }

    @Override
    public Header getContentEncoding() {
        return new BasicHeader("Content-Encoding", GZIP_CODEC);
    }

    @Override
    public long getContentLength() {
        return -1L;
    }

    @Override
    public boolean isChunked() {
        return true;
    }

    @Override
    public InputStream getContent() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        Args.notNull(outStream, "Output stream");
        GZIPOutputStream gzip = new GZIPOutputStream(outStream);
        this.wrappedEntity.writeTo(gzip);
        gzip.close();
    }
}

