/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.entity;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.DecompressingEntity;
import org.apache.http.client.entity.GZIPInputStreamFactory;

public class GzipDecompressingEntity
extends DecompressingEntity {
    public GzipDecompressingEntity(HttpEntity entity) {
        super(entity, GZIPInputStreamFactory.getInstance());
    }
}

