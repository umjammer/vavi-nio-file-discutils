
package discUtils.ntfs;

import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import discUtils.core.coreCompat.EnumSettable;


public enum FileAttributeFlags implements EnumSettable {
    ReadOnly,
    Hidden,
    System,
    _dummy_00000008,
    _dummy_00000010,
    Archive,
    Device,
    Normal,
    Temporary,
    Sparse,
    ReparsePoint,
    Compressed,
    Offline,
    NotIndexed,
    Encrypted,
    _dummy_00008000,
    _dummy_00010000,
    _dummy_00020000,
    _dummy_00040000,
    _dummy_00080000,
    _dummy_00100000,
    _dummy_00200000,
    _dummy_00400000,
    _dummy_00800000,
    _dummy_01000000,
    _dummy_02000000,
    _dummy_04000000,
    _dummy_08000000,
    Directory,
    IndexView;

    private final int value = 1 << ordinal();

    @Override public Supplier<Integer> supplier() {
        return () -> value;
    }

    @Override public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<FileAttributeFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileAttributeFlags.class)));
    }

    public static long valueOf(EnumSet<FileAttributeFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.value)).getSum();
    }

    // TODO
    public static <E extends Enum<E> & EnumSettable> EnumSet<E> cast(Class<E> clazz, EnumSet<FileAttributeFlags> flags) {
        int value = (int) valueOf(flags);
        return Arrays.stream(clazz.getEnumConstants())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(clazz)));
    }

    /** */
    private static BitSet toBitSet(EnumSet<FileAttributeFlags> flags) {
        BitSet bs = new BitSet(values().length);
        flags.forEach(e -> bs.set(e.ordinal()));
        return bs;
    }

    // TODO using name(), loop flags is fewer than loop all enums
    private static EnumSet<FileAttributeFlags> toEnumSet(BitSet flags) {
        return Arrays.stream(values())
                .filter(e -> flags.get(e.ordinal()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileAttributeFlags.class)));
    }

    /** */
    public static EnumSet<FileAttributeFlags> and(EnumSet<FileAttributeFlags> flags1, EnumSet<FileAttributeFlags> flags2) {
        BitSet bs = toBitSet(flags1);
        bs.and(toBitSet(flags2));
        return toEnumSet(bs);
    }
}
