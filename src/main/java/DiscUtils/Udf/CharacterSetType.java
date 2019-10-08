
package DiscUtils.Udf;

import java.util.Arrays;

public enum CharacterSetType {
    CharacterSet0,
    CharacterSet1,
    CharacterSet2,
    CharacterSet3,
    CharacterSet4,
    CharacterSet5,
    CharacterSet6,
    CharacterSet7,
    CharacterSet8;

    public static CharacterSetType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
