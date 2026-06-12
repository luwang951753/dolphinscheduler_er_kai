/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

public class IOUtils {
    public static String propsToString(Properties props) throws IllegalArgumentException {
        StringWriter sw = new StringWriter();
        if (props != null) {
            try {
                props.store(sw, "");
            }
            catch (IOException ex) {
                throw new IllegalArgumentException("Cannot parse props to String.", ex);
            }
        }
        return sw.toString();
    }

    public static Properties propsFromString(String source) throws IllegalArgumentException {
        Properties copy = new Properties();
        if (source != null) {
            try {
                copy.load(new StringReader(source));
            }
            catch (IOException ex) {
                throw new IllegalArgumentException("Cannot parse props from String.", ex);
            }
        }
        return copy;
    }
}

