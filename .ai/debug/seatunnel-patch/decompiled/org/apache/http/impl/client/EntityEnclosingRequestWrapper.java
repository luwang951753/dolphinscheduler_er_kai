/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ProtocolException;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.RequestWrapper;

@Deprecated
public class EntityEnclosingRequestWrapper
extends RequestWrapper
implements HttpEntityEnclosingRequest {
    private HttpEntity entity;
    private boolean consumed;

    public EntityEnclosingRequestWrapper(HttpEntityEnclosingRequest request) throws ProtocolException {
        super(request);
        this.setEntity(request.getEntity());
    }

    @Override
    public HttpEntity getEntity() {
        return this.entity;
    }

    @Override
    public void setEntity(HttpEntity entity) {
        this.entity = entity != null ? new EntityWrapper(entity) : null;
        this.consumed = false;
    }

    @Override
    public boolean expectContinue() {
        Header expect = this.getFirstHeader("Expect");
        return expect != null && "100-continue".equalsIgnoreCase(expect.getValue());
    }

    @Override
    public boolean isRepeatable() {
        return this.entity == null || this.entity.isRepeatable() || !this.consumed;
    }

    class EntityWrapper
    extends HttpEntityWrapper {
        EntityWrapper(HttpEntity entity) {
            super(entity);
        }

        @Override
        public void consumeContent() throws IOException {
            EntityEnclosingRequestWrapper.this.consumed = true;
            super.consumeContent();
        }

        @Override
        public InputStream getContent() throws IOException {
            EntityEnclosingRequestWrapper.this.consumed = true;
            return super.getContent();
        }

        @Override
        public void writeTo(OutputStream outStream) throws IOException {
            EntityEnclosingRequestWrapper.this.consumed = true;
            super.writeTo(outStream);
        }
    }
}

