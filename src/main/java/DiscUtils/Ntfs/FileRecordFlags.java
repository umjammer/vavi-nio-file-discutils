
package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import DiscUtils.Core.CoreCompat.EnumSettable;

public enum FileRecordFlags implements EnumSettable {
    None(0x0000),
    InUse(0x0001),
    IsDirectory(0x0002),
    IsMetaFile(0x0004),
    HasViewIndex(0x0008);

    private int value;

    public int getValue() {
        return value;
    }

    private FileRecordFlags(int value) {
        this.value = value;
    }

    public Supplier<Integer> supplier() {
        return this::getValue;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    };

    public static EnumSet<FileRecordFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileRecordFlags.class)));
    }

    public static long valueOf(EnumSet<FileRecordFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }

    // TODO
    public static <E extends Enum<E> & EnumSettable> EnumSet<E> cast(Class<E> clazz, EnumSet<FileRecordFlags> flags) {
        return cast(clazz, (int) valueOf(flags));
    }

    // TODO
    public static <E extends Enum<E> & EnumSettable> EnumSet<E> cast(Class<E> clazz, int value) {
        return Arrays.stream(clazz.getEnumConstants())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(clazz)));
    }
}
