
package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import DiscUtils.Core.CoreCompat.EnumSettable;

public enum FileRecordFlags implements EnumSettable {
//    None,
    InUse,
    IsDirectory,
    IsMetaFile,
    HasViewIndex;

    private int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    };

    public static EnumSet<FileRecordFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileRecordFlags.class)));
    }

    public static long valueOf(EnumSet<FileRecordFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }

    // TODO
    public static <E extends Enum<E> & EnumSettable> EnumSet<E> cast(Class<E> clazz, EnumSet<FileRecordFlags> flags) {
        int value = (int) valueOf(flags);
        return Arrays.stream(clazz.getEnumConstants())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(clazz)));
    }
}
