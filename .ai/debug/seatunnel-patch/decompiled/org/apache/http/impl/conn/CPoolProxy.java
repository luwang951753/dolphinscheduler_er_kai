/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.conn;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSession;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.impl.conn.CPoolEntry;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.HttpContext;

class CPoolProxy
implements ManagedHttpClientConnection,
HttpContext {
    private volatile CPoolEntry poolEntry;

    CPoolProxy(CPoolEntry entry) {
        this.poolEntry = entry;
    }

    CPoolEntry getPoolEntry() {
        return this.poolEntry;
    }

    CPoolEntry detach() {
        CPoolEntry local = this.poolEntry;
        this.poolEntry = null;
        return local;
    }

    ManagedHttpClientConnection getConnection() {
        CPoolEntry local = this.poolEntry;
        if (local == null) {
            return null;
        }
        return (ManagedHttpClientConnection)local.getConnection();
    }

    ManagedHttpClientConnection getValidConnection() {
        ManagedHttpClientConnection conn = this.getConnection();
        if (conn == null) {
            throw new ConnectionShutdownException();
        }
        return conn;
    }

    @Override
    public void close() throws IOException {
        CPoolEntry local = this.poolEntry;
        if (local != null) {
            local.closeConnection();
        }
    }

    @Override
    public void shutdown() throws IOException {
        CPoolEntry local = this.poolEntry;
        if (local != null) {
            local.shutdownConnection();
        }
    }

    @Override
    public boolean isOpen() {
        CPoolEntry local = this.poolEntry;
        return local != null ? !local.isClosed() : false;
    }

    @Override
    public boolean isStale() {
        ManagedHttpClientConnection conn = this.getConnection();
        return conn != null ? conn.isStale() : true;
    }

    @Override
    public void setSocketTimeout(int timeout) {
        this.getValidConnection().setSocketTimeout(timeout);
    }

    @Override
    public int getSocketTimeout() {
        return this.getValidConnection().getSocketTimeout();
    }

    @Override
    public String getId() {
        return this.getValidConnection().getId();
    }

    @Override
    public void bind(Socket socket) throws IOException {
        this.getValidConnection().bind(socket);
    }

    @Override
    public Socket getSocket() {
        return this.getValidConnection().getSocket();
    }

    @Override
    public SSLSession getSSLSession() {
        return this.getValidConnection().getSSLSession();
    }

    @Override
    public boolean isResponseAvailable(int timeout) throws IOException {
        return this.getValidConnection().isResponseAvailable(timeout);
    }

    @Override
    public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
        this.getValidConnection().sendRequestHeader(request);
    }

    @Override
    public void sendRequestEntity(HttpEntityEnclosingRequest request) throws HttpException, IOException {
        this.getValidConnection().sendRequestEntity(request);
    }

    @Override
    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
        return this.getValidConnection().receiveResponseHeader();
    }

    @Override
    public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException {
        this.getValidConnection().receiveResponseEntity(response);
    }

    @Override
    public void flush() throws IOException {
        this.getValidConnection().flush();
    }

    @Override
    public HttpConnectionMetrics getMetrics() {
        return this.getValidConnection().getMetrics();
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.getValidConnection().getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return this.getValidConnection().getLocalPort();
    }

    @Override
    public InetAddress getRemoteAddress() {
        return this.getValidConnection().getRemoteAddress();
    }

    @Override
    public int getRemotePort() {
        return this.getValidConnection().getRemotePort();
    }

    @Override
    public Object getAttribute(String id) {
        ManagedHttpClientConnection conn = this.getValidConnection();
        return conn instanceof HttpContext ? ((HttpContext)((Object)conn)).getAttribute(id) : null;
    }

    @Override
    public void setAttribute(String id, Object obj) {
        ManagedHttpClientConnection conn = this.getValidConnection();
        if (conn instanceof HttpContext) {
            ((HttpContext)((Object)conn)).setAttribute(id, obj);
        }
    }

    @Override
    public Object removeAttribute(String id) {
        ManagedHttpClientConnection conn = this.getValidConnection();
        return conn instanceof HttpContext ? ((HttpContext)((Object)conn)).removeAttribute(id) : null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CPoolProxy{");
        ManagedHttpClientConnection conn = this.getConnection();
        if (conn != null) {
            sb.append(conn);
        } else {
            sb.append("detached");
        }
        sb.append('}');
        return sb.toString();
    }

    public static HttpClientConnection newProxy(CPoolEntry poolEntry) {
        return new CPoolProxy(poolEntry);
    }

    private static CPoolProxy getProxy(HttpClientConnection conn) {
        if (!CPoolProxy.class.isInstance(conn)) {
            throw new IllegalStateException("Unexpected connection proxy class: " + conn.getClass());
        }
        return (CPoolProxy)CPoolProxy.class.cast(conn);
    }

    public static CPoolEntry getPoolEntry(HttpClientConnection proxy) {
        CPoolEntry entry = CPoolProxy.getProxy(proxy).getPoolEntry();
        if (entry == null) {
            throw new ConnectionShutdownException();
        }
        return entry;
    }

    public static CPoolEntry detach(HttpClientConnection conn) {
        return CPoolProxy.getProxy(conn).detach();
    }
}

