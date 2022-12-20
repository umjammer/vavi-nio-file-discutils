/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.coreCompat;

import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * FileAttributes.
 *
 * TODO move to dotnet4j.io
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/09 umjammer initial version <br>
 */
public enum FileAttributes implements EnumSettable {
    ReadOnly,
    Hidden,
    System,
    _dummy_00008,
    Directory,
    Archive,
    Device,
    Normal,
    Temporary,
    SparseFile,
    ReparsePoint,
    Compressed,
    Offline,
    NotContentIndexed,
    Encrypted,
    IntegrityStream,
    _dummy_10000,
    NoScrubData;

    private final int value = 1 << ordinal();

    @Override public Supplier<Integer> supplier() {
        return () -> value;
    }

    @Override public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<FileAttributes> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileAttributes.class)));
    }

    public static long valueOf(EnumSet<FileAttributes> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }

    // TODO
    public static <E extends Enum<E> & EnumSettable> EnumSet<E> cast(Class<E> clazz, EnumSet<FileAttributes> flags) {
        int value = (int) valueOf(flags);
        return Arrays.stream(clazz.getEnumConstants())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(clazz)));
    }

    /** */
    public static Map<String, Object> toMap(EnumSet<FileAttributes> flags) {
        return flags.stream().collect(Collectors.toMap(Enum::name, f -> true));
    }

    // TODO using name(), loop flags is fewer than loop all enums
    public static EnumSet<FileAttributes> toEnumSet(Map<String, Object> flags) {
        return Arrays.stream(values())
                .filter(v -> flags.containsKey(v.name()) && (Boolean) flags.get(v.name()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileAttributes.class)));
    }

    /** */
    private static BitSet toBitSet(EnumSet<FileAttributes> flags) {
        BitSet bs = new BitSet(values().length);
        flags.forEach(e -> bs.set(e.ordinal()));
        return bs;
    }

    // TODO loop flags is fewer than loop all enums
    private static EnumSet<FileAttributes> toEnumSet(BitSet flags) {
        return Arrays.stream(values())
                .filter(e -> flags.get(e.ordinal()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileAttributes.class)));
    }

    /** */
    public static EnumSet<FileAttributes> xor(EnumSet<FileAttributes> flags1, EnumSet<FileAttributes> flags2) {
        BitSet bs = toBitSet(flags1);
        bs.xor(toBitSet(flags2));
        return toEnumSet(bs);
    }

    /** */
    public static EnumSet<FileAttributes> and(EnumSet<FileAttributes> flags1, EnumSet<FileAttributes> flags2) {
        BitSet bs = toBitSet(flags1);
        bs.and(toBitSet(flags2));
        return toEnumSet(bs);
    }
}

/* */
