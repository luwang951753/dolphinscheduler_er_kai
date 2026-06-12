/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.methods;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicMarkableReference;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.utils.CloneUtils;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionReleaseTrigger;
import org.apache.http.message.AbstractHttpMessage;

public abstract class AbstractExecutionAwareRequest
extends AbstractHttpMessage
implements HttpExecutionAware,
AbortableHttpRequest,
Cloneable,
HttpRequest {
    private final AtomicMarkableReference<Cancellable> cancellableRef = new AtomicMarkableReference<Object>(null, false);

    protected AbstractExecutionAwareRequest() {
    }

    @Override
    @Deprecated
    public void setConnectionRequest(final ClientConnectionRequest connRequest) {
        this.setCancellable(new Cancellable(){

            @Override
            public boolean cancel() {
                connRequest.abortRequest();
                return true;
            }
        });
    }

    @Override
    @Deprecated
    public void setReleaseTrigger(final ConnectionReleaseTrigger releaseTrigger) {
        this.setCancellable(new Cancellable(){

            @Override
            public boolean cancel() {
                try {
                    releaseTrigger.abortConnection();
                    return true;
                }
                catch (IOException ex) {
                    return false;
                }
            }
        });
    }

    @Override
    public void abort() {
        while (!this.cancellableRef.isMarked()) {
            Cancellable actualCancellable = this.cancellableRef.getReference();
            if (!this.cancellableRef.compareAndSet(actualCancellable, actualCancellable, false, true) || actualCancellable == null) continue;
            actualCancellable.cancel();
        }
    }

    @Override
    public boolean isAborted() {
        return this.cancellableRef.isMarked();
    }

    @Override
    public void setCancellable(Cancellable cancellable) {
        Cancellable actualCancellable = this.cancellableRef.getReference();
        if (!this.cancellableRef.compareAndSet(actualCancellable, cancellable, false, false)) {
            cancellable.cancel();
        }
    }

    public Object clone() throws CloneNotSupportedException {
        AbstractExecutionAwareRequest clone = (AbstractExecutionAwareRequest)super.clone();
        clone.headergroup = CloneUtils.cloneObject(this.headergroup);
        clone.params = CloneUtils.cloneObject(this.params);
        return clone;
    }

    @Deprecated
    public void completed() {
        this.cancellableRef.set(null, false);
    }

    public void reset() {
        boolean marked;
        Cancellable actualCancellable;
        do {
            marked = this.cancellableRef.isMarked();
            actualCancellable = this.cancellableRef.getReference();
            if (actualCancellable == null) continue;
            actualCancellable.cancel();
        } while (!this.cancellableRef.compareAndSet(actualCancellable, null, marked, false));
    }
}

