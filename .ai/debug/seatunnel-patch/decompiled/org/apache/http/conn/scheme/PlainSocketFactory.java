/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.conn.scheme;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.Args;

@Deprecated
@Contract(threading=ThreadingBehavior.IMMUTABLE)
public class PlainSocketFactory
implements SocketFactory,
SchemeSocketFactory {
    private final HostNameResolver nameResolver;

    public static PlainSocketFactory getSocketFactory() {
        return new PlainSocketFactory();
    }

    @Deprecated
    public PlainSocketFactory(HostNameResolver nameResolver) {
        this.nameResolver = nameResolver;
    }

    public PlainSocketFactory() {
        this.nameResolver = null;
    }

    @Override
    public Socket createSocket(HttpParams params) {
        return new Socket();
    }

    @Override
    public Socket createSocket() {
        return new Socket();
    }

    @Override
    public Socket connectSocket(Socket socket, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpParams params) throws IOException, ConnectTimeoutException {
        Args.notNull(remoteAddress, "Remote address");
        Args.notNull(params, "HTTP parameters");
        Socket sock = socket;
        if (sock == null) {
            sock = this.createSocket();
        }
        if (localAddress != null) {
            sock.setReuseAddress(HttpConnectionParams.getSoReuseaddr(params));
            sock.bind(localAddress);
        }
        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);
        try {
            sock.setSoTimeout(soTimeout);
            sock.connect(remoteAddress, connTimeout);
        }
        catch (SocketTimeoutException ex) {
            throw new ConnectTimeoutException("Connect to " + remoteAddress + " timed out");
        }
        return sock;
    }

    @Override
    public final boolean isSecure(Socket sock) {
        return false;
    }

    @Override
    @Deprecated
    public Socket connectSocket(Socket socket, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        InetSocketAddress local = null;
        if (localAddress != null || localPort > 0) {
            local = new InetSocketAddress(localAddress, localPort > 0 ? localPort : 0);
        }
        InetAddress remoteAddress = this.nameResolver != null ? this.nameResolver.resolve(host) : InetAddress.getByName(host);
        InetSocketAddress remote = new InetSocketAddress(remoteAddress, port);
        return this.connectSocket(socket, remote, local, params);
    }
}

