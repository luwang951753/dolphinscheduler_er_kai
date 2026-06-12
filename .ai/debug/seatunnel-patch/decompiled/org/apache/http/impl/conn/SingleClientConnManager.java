/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.conn;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.RouteTracker;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.AbstractPoolEntry;
import org.apache.http.impl.conn.AbstractPooledConnAdapter;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.params.HttpParams;
import org.apache.http.util.Args;
import org.apache.http.util.Asserts;

@Deprecated
@Contract(threading=ThreadingBehavior.SAFE)
public class SingleClientConnManager
implements ClientConnectionManager {
    private final Log log = LogFactory.getLog(this.getClass());
    public static final String MISUSE_MESSAGE = "Invalid use of SingleClientConnManager: connection still allocated.\nMake sure to release the connection before allocating another one.";
    protected final SchemeRegistry schemeRegistry;
    protected final ClientConnectionOperator connOperator;
    protected final boolean alwaysShutDown;
    protected volatile PoolEntry uniquePoolEntry;
    protected volatile ConnAdapter managedConn;
    protected volatile long lastReleaseTime;
    protected volatile long connectionExpiresTime;
    protected volatile boolean isShutDown;

    @Deprecated
    public SingleClientConnManager(HttpParams params, SchemeRegistry schreg) {
        this(schreg);
    }

    public SingleClientConnManager(SchemeRegistry schreg) {
        Args.notNull(schreg, "Scheme registry");
        this.schemeRegistry = schreg;
        this.connOperator = this.createConnectionOperator(schreg);
        this.uniquePoolEntry = new PoolEntry();
        this.managedConn = null;
        this.lastReleaseTime = -1L;
        this.alwaysShutDown = false;
        this.isShutDown = false;
    }

    public SingleClientConnManager() {
        this(SchemeRegistryFactory.createDefault());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void finalize() throws Throwable {
        try {
            this.shutdown();
        }
        finally {
            super.finalize();
        }
    }

    @Override
    public SchemeRegistry getSchemeRegistry() {
        return this.schemeRegistry;
    }

    protected ClientConnectionOperator createConnectionOperator(SchemeRegistry schreg) {
        return new DefaultClientConnectionOperator(schreg);
    }

    protected final void assertStillUp() throws IllegalStateException {
        Asserts.check(!this.isShutDown, "Manager is shut down");
    }

    @Override
    public final ClientConnectionRequest requestConnection(final HttpRoute route, final Object state) {
        return new ClientConnectionRequest(){

            @Override
            public void abortRequest() {
            }

            @Override
            public ManagedClientConnection getConnection(long timeout, TimeUnit timeUnit) {
                return SingleClientConnManager.this.getConnection(route, state);
            }
        };
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ManagedClientConnection getConnection(HttpRoute route, Object state) {
        Args.notNull(route, "Route");
        this.assertStillUp();
        if (this.log.isDebugEnabled()) {
            this.log.debug("Get connection for route " + route);
        }
        SingleClientConnManager singleClientConnManager = this;
        synchronized (singleClientConnManager) {
            Asserts.check(this.managedConn == null, MISUSE_MESSAGE);
            boolean recreate = false;
            boolean shutdown = false;
            this.closeExpiredConnections();
            if (this.uniquePoolEntry.connection.isOpen()) {
                RouteTracker tracker = this.uniquePoolEntry.tracker;
                shutdown = tracker == null || !tracker.toRoute().equals(route);
            } else {
                recreate = true;
            }
            if (shutdown) {
                recreate = true;
                try {
                    this.uniquePoolEntry.shutdown();
                }
                catch (IOException iox) {
                    this.log.debug("Problem shutting down connection.", iox);
                }
            }
            if (recreate) {
                this.uniquePoolEntry = new PoolEntry();
            }
            this.managedConn = new ConnAdapter(this.uniquePoolEntry, route);
            return this.managedConn;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void releaseConnection(ManagedClientConnection conn, long validDuration, TimeUnit timeUnit) {
        ConnAdapter sca;
        Args.check(conn instanceof ConnAdapter, "Connection class mismatch, connection not obtained from this manager");
        this.assertStillUp();
        if (this.log.isDebugEnabled()) {
            this.log.debug("Releasing connection " + conn);
        }
        ConnAdapter connAdapter = sca = (ConnAdapter)conn;
        synchronized (connAdapter) {
            if (sca.poolEntry == null) {
                return;
            }
            ClientConnectionManager manager = sca.getManager();
            Asserts.check(manager == this, "Connection not obtained from this manager");
            try {
                if (sca.isOpen() && (this.alwaysShutDown || !sca.isMarkedReusable())) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Released connection open but not reusable.");
                    }
                    sca.shutdown();
                }
            }
            catch (IOException iox) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Exception shutting down released connection.", iox);
                }
            }
            finally {
                sca.detach();
                SingleClientConnManager singleClientConnManager = this;
                synchronized (singleClientConnManager) {
                    this.managedConn = null;
                    this.lastReleaseTime = System.currentTimeMillis();
                    this.connectionExpiresTime = validDuration > 0L ? timeUnit.toMillis(validDuration) + this.lastReleaseTime : Long.MAX_VALUE;
                }
            }
        }
    }

    @Override
    public void closeExpiredConnections() {
        long time = this.connectionExpiresTime;
        if (System.currentTimeMillis() >= time) {
            this.closeIdleConnections(0L, TimeUnit.MILLISECONDS);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void closeIdleConnections(long idletime, TimeUnit timeUnit) {
        this.assertStillUp();
        Args.notNull(timeUnit, "Time unit");
        SingleClientConnManager singleClientConnManager = this;
        synchronized (singleClientConnManager) {
            long cutoff;
            if (this.managedConn == null && this.uniquePoolEntry.connection.isOpen() && this.lastReleaseTime <= (cutoff = System.currentTimeMillis() - timeUnit.toMillis(idletime))) {
                try {
                    this.uniquePoolEntry.close();
                }
                catch (IOException iox) {
                    this.log.debug("Problem closing idle connection.", iox);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void shutdown() {
        this.isShutDown = true;
        SingleClientConnManager singleClientConnManager = this;
        synchronized (singleClientConnManager) {
            try {
                if (this.uniquePoolEntry != null) {
                    this.uniquePoolEntry.shutdown();
                }
            }
            catch (IOException iox) {
                this.log.debug("Problem while shutting down manager.", iox);
            }
            finally {
                this.uniquePoolEntry = null;
                this.managedConn = null;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void revokeConnection() {
        ConnAdapter conn = this.managedConn;
        if (conn == null) {
            return;
        }
        conn.detach();
        SingleClientConnManager singleClientConnManager = this;
        synchronized (singleClientConnManager) {
            try {
                this.uniquePoolEntry.shutdown();
            }
            catch (IOException iox) {
                this.log.debug("Problem while shutting down connection.", iox);
            }
        }
    }

    protected class ConnAdapter
    extends AbstractPooledConnAdapter {
        protected ConnAdapter(PoolEntry entry, HttpRoute route) {
            super((ClientConnectionManager)SingleClientConnManager.this, entry);
            this.markReusable();
            entry.route = route;
        }
    }

    protected class PoolEntry
    extends AbstractPoolEntry {
        protected PoolEntry() {
            super(SingleClientConnManager.this.connOperator, null);
        }

        protected void close() throws IOException {
            this.shutdownEntry();
            if (this.connection.isOpen()) {
                this.connection.close();
            }
        }

        protected void shutdown() throws IOException {
            this.shutdownEntry();
            if (this.connection.isOpen()) {
                this.connection.shutdown();
            }
        }
    }
}

