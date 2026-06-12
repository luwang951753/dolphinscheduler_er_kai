/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.conn;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.HttpContext;

@Deprecated
public abstract class AbstractClientConnAdapter
implements ManagedClientConnection,
HttpContext {
    private final ClientConnectionManager connManager;
    private volatile OperatedClientConnection wrappedConnection;
    private volatile boolean markedReusable;
    private volatile boolean released;
    private volatile long duration;

    protected AbstractClientConnAdapter(ClientConnectionManager mgr, OperatedClientConnection conn) {
        this.connManager = mgr;
        this.wrappedConnection = conn;
        this.markedReusable = false;
        this.released = false;
        this.duration = Long.MAX_VALUE;
    }

    protected synchronized void detach() {
        this.wrappedConnection = null;
        this.duration = Long.MAX_VALUE;
    }

    protected OperatedClientConnection getWrappedConnection() {
        return this.wrappedConnection;
    }

    protected ClientConnectionManager getManager() {
        return this.connManager;
    }

    @Deprecated
    protected final void assertNotAborted() throws InterruptedIOException {
        if (this.isReleased()) {
            throw new InterruptedIOException("Connection has been shut down");
        }
    }

    protected boolean isReleased() {
        return this.released;
    }

    protected final void assertValid(OperatedClientConnection wrappedConn) throws ConnectionShutdownException {
        if (this.isReleased() || wrappedConn == null) {
            throw new ConnectionShutdownException();
        }
    }

    @Override
    public boolean isOpen() {
        OperatedClientConnection conn = this.getWrappedConnection();
        if (conn == null) {
            return false;
        }
        return conn.isOpen();
    }

    @Override
    public boolean isStale() {
        if (this.isReleased()) {
            return true;
        }
        OperatedClientConnection conn = this.getWrappedConnection();
        if (conn == null) {
            return true;
        }
        return conn.isStale();
    }

    @Override
    public void setSocketTimeout(int timeout) {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        conn.setSocketTimeout(timeout);
    }

    @Override
    public int getSocketTimeout() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        return conn.getSocketTimeout();
    }

    @Override
    public HttpConnectionMetrics getMetrics() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        return conn.getMetrics();
    }

    @Override
    public void flush() throws IOException {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        conn.flush();
    }

    @Override
    public boolean isResponseAvailable(int timeout) throws IOException {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        return conn.isResponseAvailable(timeout);
    }

    @Override
    public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        this.unmarkReusable();
        conn.receiveResponseEntity(response);
    }

    @Override
    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        this.unmarkReusable();
        return conn.receiveResponseHeader();
    }

    @Override
    public void sendRequestEntity(HttpEntityEnclosingRequest request) throws HttpException, IOException {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        this.unmarkReusable();
        conn.sendRequestEntity(request);
    }

    @Override
    public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        this.unmarkReusable();
        conn.sendRequestHeader(request);
    }

    @Override
    public InetAddress getLocalAddress() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        return conn.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        return conn.getLocalPort();
    }

    @Override
    public InetAddress getRemoteAddress() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        return conn.getRemoteAddress();
    }

    @Override
    public int getRemotePort() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        return conn.getRemotePort();
    }

    @Override
    public boolean isSecure() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        return conn.isSecure();
    }

    @Override
    public void bind(Socket socket) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Socket getSocket() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        if (!this.isOpen()) {
            return null;
        }
        return conn.getSocket();
    }

    @Override
    public SSLSession getSSLSession() {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        if (!this.isOpen()) {
            return null;
        }
        SSLSession result = null;
        Socket sock = conn.getSocket();
        if (sock instanceof SSLSocket) {
            result = ((SSLSocket)sock).getSession();
        }
        return result;
    }

    @Override
    public void markReusable() {
        this.markedReusable = true;
    }

    @Override
    public void unmarkReusable() {
        this.markedReusable = false;
    }

    @Override
    public boolean isMarkedReusable() {
        return this.markedReusable;
    }

    @Override
    public void setIdleDuration(long duration, TimeUnit unit) {
        this.duration = duration > 0L ? unit.toMillis(duration) : -1L;
    }

    @Override
    public synchronized void releaseConnection() {
        if (this.released) {
            return;
        }
        this.released = true;
        this.connManager.releaseConnection(this, this.duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void abortConnection() {
        if (this.released) {
            return;
        }
        this.released = true;
        this.unmarkReusable();
        try {
            this.shutdown();
        }
        catch (IOException iOException) {
            // empty catch block
        }
        this.connManager.releaseConnection(this, this.duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object getAttribute(String id) {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        if (conn instanceof HttpContext) {
            return ((HttpContext)((Object)conn)).getAttribute(id);
        }
        return null;
    }

    @Override
    public Object removeAttribute(String id) {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        if (conn instanceof HttpContext) {
            return ((HttpContext)((Object)conn)).removeAttribute(id);
        }
        return null;
    }

    @Override
    public void setAttribute(String id, Object obj) {
        OperatedClientConnection conn = this.getWrappedConnection();
        this.assertValid(conn);
        if (conn instanceof HttpContext) {
            ((HttpContext)((Object)conn)).setAttribute(id, obj);
        }
    }
}

