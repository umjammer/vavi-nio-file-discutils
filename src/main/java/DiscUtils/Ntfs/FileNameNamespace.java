
package DiscUtils.Ntfs;

public enum FileNameNamespace {
    Posix,
    Win32,
    Dos,
    Win32AndDos;

    public static FileNameNamespace valueOf(int value) {
        return values()[value];
    }

    /** */
    public static <E extends Enum<E>> E cast(Class<E> clazz, Enum<FileNameNamespace> value) {
        return clazz.getEnumConstants()[value.ordinal()];
    }
}
