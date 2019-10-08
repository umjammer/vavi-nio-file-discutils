
package DiscUtils.Udf;

import java.util.Arrays;

public enum AllocationType {
    ShortDescriptors,
    LongDescriptors,
    ExtendedDescriptors,
    Embedded;

    public static AllocationType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
