/*
 * Decompiled with CFR 0.152.
 */
package org.apache.http.cookie;

import org.apache.http.annotation.Obsolete;
import org.apache.http.cookie.SetCookie;

public interface SetCookie2
extends SetCookie {
    @Obsolete
    public void setCommentURL(String var1);

    @Obsolete
    public void setPorts(int[] var1);

    @Obsolete
    public void setDiscard(boolean var1);
}

