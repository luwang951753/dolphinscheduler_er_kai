/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.impl.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.BackoffManager;
import org.apache.http.client.ConnectionBackoffStrategy;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.client.protocol.RequestAuthCache;
import org.apache.http.client.protocol.RequestClientConnControl;
import org.apache.http.client.protocol.RequestDefaultHeaders;
import org.apache.http.client.protocol.RequestExpectContinue;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.client.protocol.ResponseProcessCookies;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Lookup;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.CookieSpecRegistries;
import org.apache.http.impl.client.DefaultClientConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.DefaultUserTokenHandler;
import org.apache.http.impl.client.IdleConnectionEvictor;
import org.apache.http.impl.client.InternalHttpClient;
import org.apache.http.impl.client.NoopUserTokenHandler;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.impl.execchain.BackoffStrategyExec;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.impl.execchain.MainClientExec;
import org.apache.http.impl.execchain.ProtocolExec;
import org.apache.http.impl.execchain.RedirectExec;
import org.apache.http.impl.execchain.RetryExec;
import org.apache.http.impl.execchain.ServiceUnavailableRetryExec;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.TextUtils;
import org.apache.http.util.VersionInfo;

public class HttpClientBuilder {
    private HttpRequestExecutor requestExec;
    private HostnameVerifier hostnameVerifier;
    private LayeredConnectionSocketFactory sslSocketFactory;
    private SSLContext sslContext;
    private HttpClientConnectionManager connManager;
    private boolean connManagerShared;
    private SchemePortResolver schemePortResolver;
    private ConnectionReuseStrategy reuseStrategy;
    private ConnectionKeepAliveStrategy keepAliveStrategy;
    private AuthenticationStrategy targetAuthStrategy;
    private AuthenticationStrategy proxyAuthStrategy;
    private UserTokenHandler userTokenHandler;
    private HttpProcessor httpprocessor;
    private DnsResolver dnsResolver;
    private LinkedList<HttpRequestInterceptor> requestFirst;
    private LinkedList<HttpRequestInterceptor> requestLast;
    private LinkedList<HttpResponseInterceptor> responseFirst;
    private LinkedList<HttpResponseInterceptor> responseLast;
    private HttpRequestRetryHandler retryHandler;
    private HttpRoutePlanner routePlanner;
    private RedirectStrategy redirectStrategy;
    private ConnectionBackoffStrategy connectionBackoffStrategy;
    private BackoffManager backoffManager;
    private ServiceUnavailableRetryStrategy serviceUnavailStrategy;
    private Lookup<AuthSchemeProvider> authSchemeRegistry;
    private Lookup<CookieSpecProvider> cookieSpecRegistry;
    private Map<String, InputStreamFactory> contentDecoderMap;
    private CookieStore cookieStore;
    private CredentialsProvider credentialsProvider;
    private String userAgent;
    private HttpHost proxy;
    private Collection<? extends Header> defaultHeaders;
    private SocketConfig defaultSocketConfig;
    private ConnectionConfig defaultConnectionConfig;
    private RequestConfig defaultRequestConfig;
    private boolean evictExpiredConnections;
    private boolean evictIdleConnections;
    private long maxIdleTime;
    private TimeUnit maxIdleTimeUnit;
    private boolean systemProperties;
    private boolean redirectHandlingDisabled;
    private boolean automaticRetriesDisabled;
    private boolean contentCompressionDisabled;
    private boolean cookieManagementDisabled;
    private boolean authCachingDisabled;
    private boolean connectionStateDisabled;
    private boolean defaultUserAgentDisabled;
    private int maxConnTotal = 0;
    private int maxConnPerRoute = 0;
    private long connTimeToLive = -1L;
    private TimeUnit connTimeToLiveTimeUnit = TimeUnit.MILLISECONDS;
    private List<Closeable> closeables;
    private PublicSuffixMatcher publicSuffixMatcher;

    public static HttpClientBuilder create() {
        return new HttpClientBuilder();
    }

    protected HttpClientBuilder() {
    }

    public final HttpClientBuilder setRequestExecutor(HttpRequestExecutor requestExec) {
        this.requestExec = requestExec;
        return this;
    }

    @Deprecated
    public final HttpClientBuilder setHostnameVerifier(X509HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public final HttpClientBuilder setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public final HttpClientBuilder setPublicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
        this.publicSuffixMatcher = publicSuffixMatcher;
        return this;
    }

    @Deprecated
    public final HttpClientBuilder setSslcontext(SSLContext sslcontext) {
        return this.setSSLContext(sslcontext);
    }

    public final HttpClientBuilder setSSLContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public final HttpClientBuilder setSSLSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    public final HttpClientBuilder setMaxConnTotal(int maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
        return this;
    }

    public final HttpClientBuilder setMaxConnPerRoute(int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
        return this;
    }

    public final HttpClientBuilder setDefaultSocketConfig(SocketConfig config) {
        this.defaultSocketConfig = config;
        return this;
    }

    public final HttpClientBuilder setDefaultConnectionConfig(ConnectionConfig config) {
        this.defaultConnectionConfig = config;
        return this;
    }

    public final HttpClientBuilder setConnectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
        this.connTimeToLive = connTimeToLive;
        this.connTimeToLiveTimeUnit = connTimeToLiveTimeUnit;
        return this;
    }

    public final HttpClientBuilder setConnectionManager(HttpClientConnectionManager connManager) {
        this.connManager = connManager;
        return this;
    }

    public final HttpClientBuilder setConnectionManagerShared(boolean shared) {
        this.connManagerShared = shared;
        return this;
    }

    public final HttpClientBuilder setConnectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
        this.reuseStrategy = reuseStrategy;
        return this;
    }

    public final HttpClientBuilder setKeepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
        this.keepAliveStrategy = keepAliveStrategy;
        return this;
    }

    public final HttpClientBuilder setTargetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
        this.targetAuthStrategy = targetAuthStrategy;
        return this;
    }

    public final HttpClientBuilder setProxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
        this.proxyAuthStrategy = proxyAuthStrategy;
        return this;
    }

    public final HttpClientBuilder setUserTokenHandler(UserTokenHandler userTokenHandler) {
        this.userTokenHandler = userTokenHandler;
        return this;
    }

    public final HttpClientBuilder disableConnectionState() {
        this.connectionStateDisabled = true;
        return this;
    }

    public final HttpClientBuilder setSchemePortResolver(SchemePortResolver schemePortResolver) {
        this.schemePortResolver = schemePortResolver;
        return this;
    }

    public final HttpClientBuilder setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public final HttpClientBuilder setDefaultHeaders(Collection<? extends Header> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    public final HttpClientBuilder addInterceptorFirst(HttpResponseInterceptor itcp) {
        if (itcp == null) {
            return this;
        }
        if (this.responseFirst == null) {
            this.responseFirst = new LinkedList();
        }
        this.responseFirst.addFirst(itcp);
        return this;
    }

    public final HttpClientBuilder addInterceptorLast(HttpResponseInterceptor itcp) {
        if (itcp == null) {
            return this;
        }
        if (this.responseLast == null) {
            this.responseLast = new LinkedList();
        }
        this.responseLast.addLast(itcp);
        return this;
    }

    public final HttpClientBuilder addInterceptorFirst(HttpRequestInterceptor itcp) {
        if (itcp == null) {
            return this;
        }
        if (this.requestFirst == null) {
            this.requestFirst = new LinkedList();
        }
        this.requestFirst.addFirst(itcp);
        return this;
    }

    public final HttpClientBuilder addInterceptorLast(HttpRequestInterceptor itcp) {
        if (itcp == null) {
            return this;
        }
        if (this.requestLast == null) {
            this.requestLast = new LinkedList();
        }
        this.requestLast.addLast(itcp);
        return this;
    }

    public final HttpClientBuilder disableCookieManagement() {
        this.cookieManagementDisabled = true;
        return this;
    }

    public final HttpClientBuilder disableContentCompression() {
        this.contentCompressionDisabled = true;
        return this;
    }

    public final HttpClientBuilder disableAuthCaching() {
        this.authCachingDisabled = true;
        return this;
    }

    public final HttpClientBuilder setHttpProcessor(HttpProcessor httpprocessor) {
        this.httpprocessor = httpprocessor;
        return this;
    }

    public final HttpClientBuilder setDnsResolver(DnsResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
        return this;
    }

    public final HttpClientBuilder setRetryHandler(HttpRequestRetryHandler retryHandler) {
        this.retryHandler = retryHandler;
        return this;
    }

    public final HttpClientBuilder disableAutomaticRetries() {
        this.automaticRetriesDisabled = true;
        return this;
    }

    public final HttpClientBuilder setProxy(HttpHost proxy) {
        this.proxy = proxy;
        return this;
    }

    public final HttpClientBuilder setRoutePlanner(HttpRoutePlanner routePlanner) {
        this.routePlanner = routePlanner;
        return this;
    }

    public final HttpClientBuilder setRedirectStrategy(RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
        return this;
    }

    public final HttpClientBuilder disableRedirectHandling() {
        this.redirectHandlingDisabled = true;
        return this;
    }

    public final HttpClientBuilder setConnectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
        this.connectionBackoffStrategy = connectionBackoffStrategy;
        return this;
    }

    public final HttpClientBuilder setBackoffManager(BackoffManager backoffManager) {
        this.backoffManager = backoffManager;
        return this;
    }

    public final HttpClientBuilder setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
        this.serviceUnavailStrategy = serviceUnavailStrategy;
        return this;
    }

    public final HttpClientBuilder setDefaultCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
        return this;
    }

    public final HttpClientBuilder setDefaultCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    public final HttpClientBuilder setDefaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
        this.authSchemeRegistry = authSchemeRegistry;
        return this;
    }

    public final HttpClientBuilder setDefaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
        this.cookieSpecRegistry = cookieSpecRegistry;
        return this;
    }

    public final HttpClientBuilder setContentDecoderRegistry(Map<String, InputStreamFactory> contentDecoderMap) {
        this.contentDecoderMap = contentDecoderMap;
        return this;
    }

    public final HttpClientBuilder setDefaultRequestConfig(RequestConfig config) {
        this.defaultRequestConfig = config;
        return this;
    }

    public final HttpClientBuilder useSystemProperties() {
        this.systemProperties = true;
        return this;
    }

    public final HttpClientBuilder evictExpiredConnections() {
        this.evictExpiredConnections = true;
        return this;
    }

    @Deprecated
    public final HttpClientBuilder evictIdleConnections(Long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return this.evictIdleConnections((long)maxIdleTime, maxIdleTimeUnit);
    }

    public final HttpClientBuilder evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        this.evictIdleConnections = true;
        this.maxIdleTime = maxIdleTime;
        this.maxIdleTimeUnit = maxIdleTimeUnit;
        return this;
    }

    public final HttpClientBuilder disableDefaultUserAgent() {
        this.defaultUserAgentDisabled = true;
        return this;
    }

    protected ClientExecChain createMainExec(HttpRequestExecutor requestExec, HttpClientConnectionManager connManager, ConnectionReuseStrategy reuseStrategy, ConnectionKeepAliveStrategy keepAliveStrategy, HttpProcessor proxyHttpProcessor, AuthenticationStrategy targetAuthStrategy, AuthenticationStrategy proxyAuthStrategy, UserTokenHandler userTokenHandler) {
        return new MainClientExec(requestExec, connManager, reuseStrategy, keepAliveStrategy, proxyHttpProcessor, targetAuthStrategy, proxyAuthStrategy, userTokenHandler);
    }

    protected ClientExecChain decorateMainExec(ClientExecChain mainExec) {
        return mainExec;
    }

    protected ClientExecChain decorateProtocolExec(ClientExecChain protocolExec) {
        return protocolExec;
    }

    protected void addCloseable(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        if (this.closeables == null) {
            this.closeables = new ArrayList<Closeable>();
        }
        this.closeables.add(closeable);
    }

    private static String[] split(String s) {
        if (TextUtils.isBlank(s)) {
            return null;
        }
        return s.split(" *, *");
    }

    /*
     * WARNING - void declaration
     */
    public CloseableHttpClient build() {
        void var14_31;
        ArrayList<Closeable> closeablesCopy;
        CredentialsProvider defaultCredentialsProvider;
        CookieStore defaultCookieStore;
        Lookup<CookieSpecProvider> cookieSpecRegistryCopy;
        Lookup<AuthSchemeProvider> lookup;
        ServiceUnavailableRetryStrategy serviceUnavailStrategyCopy;
        HttpRoutePlanner routePlannerCopy;
        String userAgentCopy;
        UserTokenHandler userTokenHandlerCopy;
        AuthenticationStrategy proxyAuthStrategyCopy;
        AuthenticationStrategy targetAuthStrategyCopy;
        ConnectionKeepAliveStrategy keepAliveStrategyCopy;
        ConnectionReuseStrategy reuseStrategyCopy;
        HttpClientConnectionManager connManagerCopy;
        HttpRequestExecutor requestExecCopy;
        PublicSuffixMatcher publicSuffixMatcherCopy = this.publicSuffixMatcher;
        if (publicSuffixMatcherCopy == null) {
            publicSuffixMatcherCopy = PublicSuffixMatcherLoader.getDefault();
        }
        if ((requestExecCopy = this.requestExec) == null) {
            requestExecCopy = new HttpRequestExecutor();
        }
        if ((connManagerCopy = this.connManager) == null) {
            String s;
            LayeredConnectionSocketFactory sslSocketFactoryCopy = this.sslSocketFactory;
            if (sslSocketFactoryCopy == null) {
                String[] supportedProtocols = this.systemProperties ? HttpClientBuilder.split(System.getProperty("https.protocols")) : null;
                String[] supportedCipherSuites = this.systemProperties ? HttpClientBuilder.split(System.getProperty("https.cipherSuites")) : null;
                HostnameVerifier hostnameVerifierCopy = this.hostnameVerifier;
                if (hostnameVerifierCopy == null) {
                    hostnameVerifierCopy = new DefaultHostnameVerifier(publicSuffixMatcherCopy);
                }
                sslSocketFactoryCopy = this.sslContext != null ? new SSLConnectionSocketFactory(this.sslContext, supportedProtocols, supportedCipherSuites, hostnameVerifierCopy) : (this.systemProperties ? new SSLConnectionSocketFactory((SSLSocketFactory)SSLSocketFactory.getDefault(), supportedProtocols, supportedCipherSuites, hostnameVerifierCopy) : new SSLConnectionSocketFactory(SSLContexts.createDefault(), hostnameVerifierCopy));
            }
            PoolingHttpClientConnectionManager poolingmgr = new PoolingHttpClientConnectionManager(RegistryBuilder.create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", (PlainConnectionSocketFactory)((Object)sslSocketFactoryCopy)).build(), null, null, this.dnsResolver, this.connTimeToLive, this.connTimeToLiveTimeUnit != null ? this.connTimeToLiveTimeUnit : TimeUnit.MILLISECONDS);
            if (this.defaultSocketConfig != null) {
                poolingmgr.setDefaultSocketConfig(this.defaultSocketConfig);
            }
            if (this.defaultConnectionConfig != null) {
                poolingmgr.setDefaultConnectionConfig(this.defaultConnectionConfig);
            }
            if (this.systemProperties && "true".equalsIgnoreCase(s = System.getProperty("http.keepAlive", "true"))) {
                s = System.getProperty("http.maxConnections", "5");
                int max = Integer.parseInt(s);
                poolingmgr.setDefaultMaxPerRoute(max);
                poolingmgr.setMaxTotal(2 * max);
            }
            if (this.maxConnTotal > 0) {
                poolingmgr.setMaxTotal(this.maxConnTotal);
            }
            if (this.maxConnPerRoute > 0) {
                poolingmgr.setDefaultMaxPerRoute(this.maxConnPerRoute);
            }
            connManagerCopy = poolingmgr;
        }
        if ((reuseStrategyCopy = this.reuseStrategy) == null) {
            String s;
            reuseStrategyCopy = this.systemProperties ? ("true".equalsIgnoreCase(s = System.getProperty("http.keepAlive", "true")) ? DefaultClientConnectionReuseStrategy.INSTANCE : NoConnectionReuseStrategy.INSTANCE) : DefaultClientConnectionReuseStrategy.INSTANCE;
        }
        if ((keepAliveStrategyCopy = this.keepAliveStrategy) == null) {
            keepAliveStrategyCopy = DefaultConnectionKeepAliveStrategy.INSTANCE;
        }
        if ((targetAuthStrategyCopy = this.targetAuthStrategy) == null) {
            targetAuthStrategyCopy = TargetAuthenticationStrategy.INSTANCE;
        }
        if ((proxyAuthStrategyCopy = this.proxyAuthStrategy) == null) {
            proxyAuthStrategyCopy = ProxyAuthenticationStrategy.INSTANCE;
        }
        if ((userTokenHandlerCopy = this.userTokenHandler) == null) {
            userTokenHandlerCopy = !this.connectionStateDisabled ? DefaultUserTokenHandler.INSTANCE : NoopUserTokenHandler.INSTANCE;
        }
        if ((userAgentCopy = this.userAgent) == null) {
            if (this.systemProperties) {
                userAgentCopy = System.getProperty("http.agent");
            }
            if (userAgentCopy == null && !this.defaultUserAgentDisabled) {
                userAgentCopy = VersionInfo.getUserAgent("Apache-HttpClient", "org.apache.http.client", this.getClass());
            }
        }
        ClientExecChain execChain = this.createMainExec(requestExecCopy, connManagerCopy, reuseStrategyCopy, keepAliveStrategyCopy, new ImmutableHttpProcessor(new RequestTargetHost(), new RequestUserAgent(userAgentCopy)), targetAuthStrategyCopy, proxyAuthStrategyCopy, userTokenHandlerCopy);
        execChain = this.decorateMainExec(execChain);
        HttpProcessor httpprocessorCopy = this.httpprocessor;
        if (httpprocessorCopy == null) {
            HttpProcessorBuilder b = HttpProcessorBuilder.create();
            if (this.requestFirst != null) {
                for (HttpRequestInterceptor httpRequestInterceptor : this.requestFirst) {
                    b.addFirst(httpRequestInterceptor);
                }
            }
            if (this.responseFirst != null) {
                for (HttpResponseInterceptor httpResponseInterceptor : this.responseFirst) {
                    b.addFirst(httpResponseInterceptor);
                }
            }
            b.addAll(new RequestDefaultHeaders(this.defaultHeaders), new RequestContent(), new RequestTargetHost(), new RequestClientConnControl(), new RequestUserAgent(userAgentCopy), new RequestExpectContinue());
            if (!this.cookieManagementDisabled) {
                b.add(new RequestAddCookies());
            }
            if (!this.contentCompressionDisabled) {
                if (this.contentDecoderMap != null) {
                    ArrayList<String> encodings = new ArrayList<String>(this.contentDecoderMap.keySet());
                    Collections.sort(encodings);
                    b.add(new RequestAcceptEncoding(encodings));
                } else {
                    b.add(new RequestAcceptEncoding());
                }
            }
            if (!this.authCachingDisabled) {
                b.add(new RequestAuthCache());
            }
            if (!this.cookieManagementDisabled) {
                b.add(new ResponseProcessCookies());
            }
            if (!this.contentCompressionDisabled) {
                if (this.contentDecoderMap != null) {
                    RegistryBuilder<InputStreamFactory> b2 = RegistryBuilder.create();
                    for (Map.Entry<String, InputStreamFactory> entry : this.contentDecoderMap.entrySet()) {
                        b2.register(entry.getKey(), entry.getValue());
                    }
                    b.add(new ResponseContentEncoding(b2.build()));
                } else {
                    b.add(new ResponseContentEncoding());
                }
            }
            if (this.requestLast != null) {
                for (HttpRequestInterceptor httpRequestInterceptor : this.requestLast) {
                    b.addLast(httpRequestInterceptor);
                }
            }
            if (this.responseLast != null) {
                for (HttpResponseInterceptor httpResponseInterceptor : this.responseLast) {
                    b.addLast(httpResponseInterceptor);
                }
            }
            httpprocessorCopy = b.build();
        }
        execChain = new ProtocolExec(execChain, httpprocessorCopy);
        execChain = this.decorateProtocolExec(execChain);
        if (!this.automaticRetriesDisabled) {
            HttpRequestRetryHandler retryHandlerCopy = this.retryHandler;
            if (retryHandlerCopy == null) {
                retryHandlerCopy = DefaultHttpRequestRetryHandler.INSTANCE;
            }
            execChain = new RetryExec(execChain, retryHandlerCopy);
        }
        if ((routePlannerCopy = this.routePlanner) == null) {
            SchemePortResolver schemePortResolverCopy = this.schemePortResolver;
            if (schemePortResolverCopy == null) {
                schemePortResolverCopy = DefaultSchemePortResolver.INSTANCE;
            }
            routePlannerCopy = this.proxy != null ? new DefaultProxyRoutePlanner(this.proxy, schemePortResolverCopy) : (this.systemProperties ? new SystemDefaultRoutePlanner(schemePortResolverCopy, ProxySelector.getDefault()) : new DefaultRoutePlanner(schemePortResolverCopy));
        }
        if ((serviceUnavailStrategyCopy = this.serviceUnavailStrategy) != null) {
            execChain = new ServiceUnavailableRetryExec(execChain, serviceUnavailStrategyCopy);
        }
        if (!this.redirectHandlingDisabled) {
            void var14_27;
            RedirectStrategy redirectStrategy = this.redirectStrategy;
            if (redirectStrategy == null) {
                DefaultRedirectStrategy defaultRedirectStrategy = DefaultRedirectStrategy.INSTANCE;
            }
            execChain = new RedirectExec(execChain, routePlannerCopy, (RedirectStrategy)var14_27);
        }
        if (this.backoffManager != null && this.connectionBackoffStrategy != null) {
            execChain = new BackoffStrategyExec(execChain, this.connectionBackoffStrategy, this.backoffManager);
        }
        if ((lookup = this.authSchemeRegistry) == null) {
            Registry<BasicSchemeFactory> registry = RegistryBuilder.create().register("Basic", new BasicSchemeFactory()).register("Digest", (BasicSchemeFactory)((Object)new DigestSchemeFactory())).register("NTLM", (BasicSchemeFactory)((Object)new NTLMSchemeFactory())).register("Negotiate", (BasicSchemeFactory)((Object)new SPNegoSchemeFactory())).register("Kerberos", (BasicSchemeFactory)((Object)new KerberosSchemeFactory())).build();
        }
        if ((cookieSpecRegistryCopy = this.cookieSpecRegistry) == null) {
            cookieSpecRegistryCopy = CookieSpecRegistries.createDefault(publicSuffixMatcherCopy);
        }
        if ((defaultCookieStore = this.cookieStore) == null) {
            defaultCookieStore = new BasicCookieStore();
        }
        if ((defaultCredentialsProvider = this.credentialsProvider) == null) {
            defaultCredentialsProvider = this.systemProperties ? new SystemDefaultCredentialsProvider() : new BasicCredentialsProvider();
        }
        ArrayList<Closeable> arrayList = closeablesCopy = this.closeables != null ? new ArrayList<Closeable>(this.closeables) : null;
        if (!this.connManagerShared) {
            if (closeablesCopy == null) {
                closeablesCopy = new ArrayList(1);
            }
            final HttpClientConnectionManager cm = connManagerCopy;
            if (this.evictExpiredConnections || this.evictIdleConnections) {
                final IdleConnectionEvictor connectionEvictor = new IdleConnectionEvictor(cm, this.maxIdleTime > 0L ? this.maxIdleTime : 10L, this.maxIdleTimeUnit != null ? this.maxIdleTimeUnit : TimeUnit.SECONDS, this.maxIdleTime, this.maxIdleTimeUnit);
                closeablesCopy.add(new Closeable(){

                    @Override
                    public void close() throws IOException {
                        connectionEvictor.shutdown();
                        try {
                            connectionEvictor.awaitTermination(1L, TimeUnit.SECONDS);
                        }
                        catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                connectionEvictor.start();
            }
            closeablesCopy.add(new Closeable(){

                @Override
                public void close() throws IOException {
                    cm.shutdown();
                }
            });
        }
        return new InternalHttpClient(execChain, connManagerCopy, routePlannerCopy, cookieSpecRegistryCopy, (Lookup<AuthSchemeProvider>)var14_31, defaultCookieStore, defaultCredentialsProvider, this.defaultRequestConfig != null ? this.defaultRequestConfig : RequestConfig.DEFAULT, closeablesCopy);
    }
}

