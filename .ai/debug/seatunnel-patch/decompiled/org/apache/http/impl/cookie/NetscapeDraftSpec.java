/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.annotation.Contract
 *  org.apache.http.annotation.ThreadingBehavior
 */
package org.apache.http.impl.cookie;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.Obsolete;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.cookie.BasicCommentHandler;
import org.apache.http.impl.cookie.BasicExpiresHandler;
import org.apache.http.impl.cookie.BasicPathHandler;
import org.apache.http.impl.cookie.BasicSecureHandler;
import org.apache.http.impl.cookie.CookieSpecBase;
import org.apache.http.impl.cookie.NetscapeDomainHandler;
import org.apache.http.impl.cookie.NetscapeDraftHeaderParser;
import org.apache.http.message.BufferedHeader;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.Args;
import org.apache.http.util.CharArrayBuffer;

@Obsolete
@Contract(threading=ThreadingBehavior.SAFE)
public class NetscapeDraftSpec
extends CookieSpecBase {
    protected static final String EXPIRES_PATTERN = "EEE, dd-MMM-yy HH:mm:ss z";

    public NetscapeDraftSpec(String[] datepatterns) {
        String[] stringArray;
        CommonCookieAttributeHandler[] commonCookieAttributeHandlerArray = new CommonCookieAttributeHandler[5];
        commonCookieAttributeHandlerArray[0] = new BasicPathHandler();
        commonCookieAttributeHandlerArray[1] = new NetscapeDomainHandler();
        commonCookieAttributeHandlerArray[2] = new BasicSecureHandler();
        commonCookieAttributeHandlerArray[3] = new BasicCommentHandler();
        if (datepatterns != null) {
            stringArray = (String[])datepatterns.clone();
        } else {
            String[] stringArray2 = new String[1];
            stringArray = stringArray2;
            stringArray2[0] = EXPIRES_PATTERN;
        }
        commonCookieAttributeHandlerArray[4] = new BasicExpiresHandler(stringArray);
        super(commonCookieAttributeHandlerArray);
    }

    NetscapeDraftSpec(CommonCookieAttributeHandler ... handlers) {
        super(handlers);
    }

    public NetscapeDraftSpec() {
        this((String[])null);
    }

    @Override
    public List<Cookie> parse(Header header, CookieOrigin origin) throws MalformedCookieException {
        ParserCursor cursor;
        CharArrayBuffer buffer;
        Args.notNull(header, "Header");
        Args.notNull(origin, "Cookie origin");
        if (!header.getName().equalsIgnoreCase("Set-Cookie")) {
            throw new MalformedCookieException("Unrecognized cookie header '" + header.toString() + "'");
        }
        NetscapeDraftHeaderParser parser = NetscapeDraftHeaderParser.DEFAULT;
        if (header instanceof FormattedHeader) {
            buffer = ((FormattedHeader)header).getBuffer();
            cursor = new ParserCursor(((FormattedHeader)header).getValuePos(), buffer.length());
        } else {
            String s = header.getValue();
            if (s == null) {
                throw new MalformedCookieException("Header value is null");
            }
            buffer = new CharArrayBuffer(s.length());
            buffer.append(s);
            cursor = new ParserCursor(0, buffer.length());
        }
        return this.parse(new HeaderElement[]{parser.parseHeader(buffer, cursor)}, origin);
    }

    @Override
    public List<Header> formatCookies(List<Cookie> cookies) {
        Args.notEmpty(cookies, "List of cookies");
        CharArrayBuffer buffer = new CharArrayBuffer(20 * cookies.size());
        buffer.append("Cookie");
        buffer.append(": ");
        for (int i = 0; i < cookies.size(); ++i) {
            Cookie cookie = cookies.get(i);
            if (i > 0) {
                buffer.append("; ");
            }
            buffer.append(cookie.getName());
            String s = cookie.getValue();
            if (s == null) continue;
            buffer.append("=");
            buffer.append(s);
        }
        ArrayList<Header> headers = new ArrayList<Header>(1);
        headers.add(new BufferedHeader(buffer));
        return headers;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public Header getVersionHeader() {
        return null;
    }

    public String toString() {
        return "netscape";
    }
}

