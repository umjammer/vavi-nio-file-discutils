
package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import DiscUtils.Core.CoreCompat.EnumSettable;


public enum FileAttributeFlags implements EnumSettable {
    None(0x00000000),
    ReadOnly(0x00000001),
    Hidden(0x00000002),
    System(0x00000004),
    Archive(0x00000020),
    Device(0x00000040),
    Normal(0x00000080),
    Temporary(0x00000100),
    Sparse(0x00000200),
    ReparsePoint(0x00000400),
    Compressed(0x00000800),
    Offline(0x00001000),
    NotIndexed(0x00002000),
    Encrypted(0x00004000),
    Directory(0x10000000),
    IndexView(0x20000000);

    private int value;

    public int getValue() {
        return value;
    }

    private FileAttributeFlags(int value) {
        this.value = value;
    }

    // TODO
    public Supplier<Integer> supplier() {
        return this::getValue;
    }

    // TODO
    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    };

    public static EnumSet<FileAttributeFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileAttributeFlags.class)));
    }

    public static long valueOf(EnumSet<FileAttributeFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }

    // TODO
    public static <E extends Enum<E> & EnumSettable> EnumSet<E> cast(Class<E> clazz, EnumSet<FileAttributeFlags> flags) {
        return cast(clazz, (int) valueOf(flags));
    }

    // TODO
    public static <E extends Enum<E> & EnumSettable> EnumSet<E> cast(Class<E> clazz, int value) {
        return Arrays.stream(clazz.getEnumConstants())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(clazz)));
    }

    // TODO using name()
    public static Map<String, Object> toMap(EnumSet<FileAttributeFlags> flags) {
        return flags.stream().collect(Collectors.toMap(f -> f.name(), f -> true));
    }

    // TODO using name(), loop flags is fewer than loop all enums
    public static EnumSet<FileAttributeFlags> toEnumSet(Map<String, Object> flags) {
        return Arrays.stream(values())
                .filter(v -> Boolean.class.cast(flags.get(v.name())))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileAttributeFlags.class)));
    }

    /** TODO using name() */
    public static EnumSet<FileAttributeFlags> and(Map<String, Object> attrs, Map<String, Object> mask) {
        return mask.entrySet()
                .stream()
                .filter(e -> attrs.containsKey(e.getKey()) && attrs.get(e.getKey()) == e.getValue())
                .map(e -> valueOf(e.getKey()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileAttributeFlags.class)));
    }

    /** TODO */
    public static EnumSet<FileAttributeFlags> and(EnumSet<FileAttributeFlags> flags, int mask) {
        return and(toMap(flags), toMap(valueOf(mask)));
    }
}
