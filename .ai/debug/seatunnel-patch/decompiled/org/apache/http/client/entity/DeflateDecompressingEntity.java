/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.client.entity;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.DecompressingEntity;
import org.apache.http.client.entity.DeflateInputStreamFactory;

public class DeflateDecompressingEntity
extends DecompressingEntity {
    public DeflateDecompressingEntity(HttpEntity entity) {
        super(entity, DeflateInputStreamFactory.getInstance());
    }
}

