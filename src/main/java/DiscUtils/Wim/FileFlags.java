
package DiscUtils.Wim;

import java.util.Arrays;

public enum FileFlags {
    Compression,
    ReadOnly,
    Spanned,
    ResourceOnly,
    MetaDataOnly,
    WriteInProgress,
    ReparsePointFix,
    XpressCompression,
    LzxCompression;

    public static FileFlags valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
