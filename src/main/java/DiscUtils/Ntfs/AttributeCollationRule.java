
package DiscUtils.Ntfs;

import java.util.Arrays;

public enum AttributeCollationRule {
    Binary(0x00000000),
    Filename(0x00000001),
    UnicodeString(0x00000002),
    UnsignedLong(0x00000010),
    Sid(0x00000011),
    SecurityHash(0x00000012),
    MultipleUnsignedLongs(0x00000013);

    private int value;

    public int getValue() {
        return value;
    }

    private AttributeCollationRule(int value) {
        this.value = value;
    }

    public static AttributeCollationRule valueOf(int value) {
        return Arrays.stream(values()).filter(e -> e.getValue() == value).findFirst().get();
    }
}
