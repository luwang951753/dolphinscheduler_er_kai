/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.sink;

import com.google.common.base.Preconditions;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

public class HttpPutBuilder {
    String url;
    Map<String, String> header = new HashMap<String, String>();
    HttpEntity httpEntity;

    public HttpPutBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    public HttpPutBuilder addCommonHeader() {
        this.header.put("Expect", "100-continue");
        this.header.put("Content-Type", "text/plain");
        return this;
    }

    public HttpPutBuilder addHiddenColumns(boolean add) {
        if (add) {
            this.header.put("hidden_columns", "__DORIS_DELETE_SIGN__");
        }
        return this;
    }

    public HttpPutBuilder enable2PC() {
        this.header.put("two_phase_commit", "true");
        return this;
    }

    public HttpPutBuilder baseAuth(String user, String password) {
        String authInfo = user + ":" + password;
        byte[] encoded = Base64.encodeBase64(authInfo.getBytes(StandardCharsets.UTF_8));
        this.header.put("Authorization", "Basic " + new String(encoded));
        return this;
    }

    public HttpPutBuilder addTxnId(long txnID) {
        this.header.put("txn_id", String.valueOf(txnID));
        return this;
    }

    public HttpPutBuilder commit() {
        this.header.put("txn_operation", "commit");
        return this;
    }

    public HttpPutBuilder abort() {
        this.header.put("txn_operation", "abort");
        return this;
    }

    public HttpPutBuilder setEntity(HttpEntity httpEntity) {
        this.httpEntity = httpEntity;
        return this;
    }

    public HttpPutBuilder setEmptyEntity() {
        try {
            this.httpEntity = new StringEntity("");
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    public HttpPutBuilder addProperties(Properties properties) {
        properties.forEach((BiConsumer<? super Object, ? super Object>)((BiConsumer<Object, Object>)(key, value) -> this.header.put(String.valueOf(key), String.valueOf(value))));
        return this;
    }

    public HttpPutBuilder setLabel(String label) {
        this.header.put("label", label);
        return this;
    }

    public HttpPut build() {
        Preconditions.checkNotNull(this.url);
        Preconditions.checkNotNull(this.httpEntity);
        HttpPut put = new HttpPut(this.url);
        this.header.forEach(put::setHeader);
        put.setEntity(this.httpEntity);
        return put;
    }
}

