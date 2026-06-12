/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.conn.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SubjectName;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.util.Args;

@Deprecated
public abstract class AbstractVerifier
implements X509HostnameVerifier {
    private final Log log = LogFactory.getLog(this.getClass());
    static final String[] BAD_COUNTRY_2LDS = new String[]{"ac", "co", "com", "ed", "edu", "go", "gouv", "gov", "info", "lg", "ne", "net", "or", "org"};

    @Override
    public final void verify(String host, SSLSocket ssl) throws IOException {
        Args.notNull(host, "Host");
        SSLSession session = ssl.getSession();
        if (session == null) {
            InputStream in = ssl.getInputStream();
            in.available();
            session = ssl.getSession();
            if (session == null) {
                ssl.startHandshake();
                session = ssl.getSession();
            }
        }
        Certificate[] certs = session.getPeerCertificates();
        X509Certificate x509 = (X509Certificate)certs[0];
        this.verify(host, x509);
    }

    @Override
    public final boolean verify(String host, SSLSession session) {
        try {
            Certificate[] certs = session.getPeerCertificates();
            X509Certificate x509 = (X509Certificate)certs[0];
            this.verify(host, x509);
            return true;
        }
        catch (SSLException ex) {
            if (this.log.isDebugEnabled()) {
                this.log.debug(ex.getMessage(), ex);
            }
            return false;
        }
    }

    @Override
    public final void verify(String host, X509Certificate cert) throws SSLException {
        String[] stringArray;
        X500Principal subjectPrincipal;
        String cn;
        List<SubjectName> allSubjectAltNames = DefaultHostnameVerifier.getSubjectAltNames(cert);
        ArrayList<String> subjectAlts = new ArrayList<String>();
        if (InetAddressUtils.isIPv4Address(host) || InetAddressUtils.isIPv6Address(host)) {
            for (SubjectName subjectName : allSubjectAltNames) {
                if (subjectName.getType() != 7) continue;
                subjectAlts.add(subjectName.getValue());
            }
        } else {
            for (SubjectName subjectName : allSubjectAltNames) {
                if (subjectName.getType() != 2) continue;
                subjectAlts.add(subjectName.getValue());
            }
        }
        if ((cn = DefaultHostnameVerifier.extractCN((subjectPrincipal = cert.getSubjectX500Principal()).getName("RFC2253"))) != null) {
            String[] stringArray2 = new String[1];
            stringArray = stringArray2;
            stringArray2[0] = cn;
        } else {
            stringArray = null;
        }
        this.verify(host, stringArray, subjectAlts != null && !subjectAlts.isEmpty() ? subjectAlts.toArray(new String[subjectAlts.size()]) : null);
    }

    public final void verify(String host, String[] cns, String[] subjectAlts, boolean strictWithSubDomains) throws SSLException {
        String normalizedHost;
        String cn = cns != null && cns.length > 0 ? cns[0] : null;
        List<String> subjectAltList = subjectAlts != null && subjectAlts.length > 0 ? Arrays.asList(subjectAlts) : null;
        String string = normalizedHost = InetAddressUtils.isIPv6Address(host) ? DefaultHostnameVerifier.normaliseAddress(host.toLowerCase(Locale.ROOT)) : host;
        if (subjectAltList != null) {
            for (String subjectAlt : subjectAltList) {
                String normalizedAltSubject = InetAddressUtils.isIPv6Address(subjectAlt) ? DefaultHostnameVerifier.normaliseAddress(subjectAlt) : subjectAlt;
                if (!AbstractVerifier.matchIdentity(normalizedHost, normalizedAltSubject, strictWithSubDomains)) continue;
                return;
            }
            throw new SSLException("Certificate for <" + host + "> doesn't match any " + "of the subject alternative names: " + subjectAltList);
        }
        if (cn != null) {
            String normalizedCN;
            String string2 = normalizedCN = InetAddressUtils.isIPv6Address(cn) ? DefaultHostnameVerifier.normaliseAddress(cn) : cn;
            if (AbstractVerifier.matchIdentity(normalizedHost, normalizedCN, strictWithSubDomains)) {
                return;
            }
            throw new SSLException("Certificate for <" + host + "> doesn't match " + "common name of the certificate subject: " + cn);
        }
        throw new SSLException("Certificate subject for <" + host + "> doesn't contain " + "a common name and does not have alternative names");
    }

    private static boolean matchIdentity(String host, String identity, boolean strict) {
        boolean doWildcard;
        if (host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        String normalizedIdentity = identity.toLowerCase(Locale.ROOT);
        String[] parts = normalizedIdentity.split("\\.");
        boolean bl = doWildcard = parts.length >= 3 && parts[0].endsWith("*") && (!strict || AbstractVerifier.validCountryWildcard(parts));
        if (doWildcard) {
            boolean match;
            String firstpart = parts[0];
            if (firstpart.length() > 1) {
                String prefix = firstpart.substring(0, firstpart.length() - 1);
                String suffix = normalizedIdentity.substring(firstpart.length());
                String hostSuffix = normalizedHost.substring(prefix.length());
                match = normalizedHost.startsWith(prefix) && hostSuffix.endsWith(suffix);
            } else {
                match = normalizedHost.endsWith(normalizedIdentity.substring(1));
            }
            return match && (!strict || AbstractVerifier.countDots(normalizedHost) == AbstractVerifier.countDots(normalizedIdentity));
        }
        return normalizedHost.equals(normalizedIdentity);
    }

    private static boolean validCountryWildcard(String[] parts) {
        if (parts.length != 3 || parts[2].length() != 2) {
            return true;
        }
        return Arrays.binarySearch(BAD_COUNTRY_2LDS, parts[1]) < 0;
    }

    public static boolean acceptableCountryWildcard(String cn) {
        return AbstractVerifier.validCountryWildcard(cn.split("\\."));
    }

    public static String[] getCNs(X509Certificate cert) {
        String subjectPrincipal = cert.getSubjectX500Principal().toString();
        try {
            String[] stringArray;
            String cn = DefaultHostnameVerifier.extractCN(subjectPrincipal);
            if (cn != null) {
                String[] stringArray2 = new String[1];
                stringArray = stringArray2;
                stringArray2[0] = cn;
            } else {
                stringArray = null;
            }
            return stringArray;
        }
        catch (SSLException ex) {
            return null;
        }
    }

    public static String[] getDNSSubjectAlts(X509Certificate cert) {
        List<SubjectName> subjectAltNames = DefaultHostnameVerifier.getSubjectAltNames(cert);
        if (subjectAltNames == null) {
            return null;
        }
        ArrayList<String> dnsAlts = new ArrayList<String>();
        for (SubjectName subjectName : subjectAltNames) {
            if (subjectName.getType() != 2) continue;
            dnsAlts.add(subjectName.getValue());
        }
        return dnsAlts.isEmpty() ? dnsAlts.toArray(new String[dnsAlts.size()]) : null;
    }

    public static int countDots(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) != '.') continue;
            ++count;
        }
        return count;
    }

    static {
        Arrays.sort(BAD_COUNTRY_2LDS);
    }
}

