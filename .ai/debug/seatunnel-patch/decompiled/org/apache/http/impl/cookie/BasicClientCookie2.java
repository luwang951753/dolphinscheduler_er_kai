/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.cookie;

import java.util.Date;
import org.apache.http.cookie.SetCookie2;
import org.apache.http.impl.cookie.BasicClientCookie;

public class BasicClientCookie2
extends BasicClientCookie
implements SetCookie2 {
    private static final long serialVersionUID = -7744598295706617057L;
    private String commentURL;
    private int[] ports;
    private boolean discard;

    public BasicClientCookie2(String name, String value) {
        super(name, value);
    }

    @Override
    public int[] getPorts() {
        return this.ports;
    }

    @Override
    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    @Override
    public String getCommentURL() {
        return this.commentURL;
    }

    @Override
    public void setCommentURL(String commentURL) {
        this.commentURL = commentURL;
    }

    @Override
    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    @Override
    public boolean isPersistent() {
        return !this.discard && super.isPersistent();
    }

    @Override
    public boolean isExpired(Date date) {
        return this.discard || super.isExpired(date);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        BasicClientCookie2 clone = (BasicClientCookie2)super.clone();
        if (this.ports != null) {
            clone.ports = (int[])this.ports.clone();
        }
        return clone;
    }
}

