
package DiscUtils.Ntfs;

import java.util.Arrays;

public enum FileNameNamespace {
    Posix,
    Win32,
    Dos,
    Win32AndDos;

    public static FileNameNamespace valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }

    /** */
    public static <E extends Enum<E>> E cast(Class<E> clazz, Enum<FileNameNamespace> value) {
        return Arrays.stream(clazz.getEnumConstants()).filter(e -> e.ordinal() == value.ordinal()).findFirst().get();
    }
}
