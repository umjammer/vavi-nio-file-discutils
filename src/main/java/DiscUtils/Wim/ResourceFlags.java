
package DiscUtils.Wim;

public enum ResourceFlags {
    None,
    Free,
    MetaData,
    __dummyEnum__0,
    Compressed,
    __dummyEnum__1,
    __dummyEnum__2,
    __dummyEnum__3,
    Spanned;

    public static ResourceFlags valueOf(int value) {
        return values()[value];
    }
}
