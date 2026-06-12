/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.io.file;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.DeleteOption;

public enum StandardDeleteOption implements DeleteOption
{
    OVERRIDE_READ_ONLY;


    public static boolean overrideReadOnly(DeleteOption[] options) {
        if (IOUtils.length(options) == 0) {
            return false;
        }
        for (DeleteOption deleteOption : options) {
            if (deleteOption != OVERRIDE_READ_ONLY) continue;
            return true;
        }
        return false;
    }
}

