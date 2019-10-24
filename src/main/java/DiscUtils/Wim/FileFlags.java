
package DiscUtils.Wim;

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
        return values()[value];
    }
}
