
package moe.yo3explorer.dotnetio4j.compat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;


public enum ObjectAceFlags {
    None,
    ObjectAceTypePresent,
    InheritedObjectAceTypePresent;

    public static EnumSet<ObjectAceFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (v.ordinal() & value) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ObjectAceFlags.class)));
    }

    public static long valueOf(EnumSet<ObjectAceFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
