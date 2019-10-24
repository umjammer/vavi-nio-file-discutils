
package DiscUtils.Ntfs;

public enum AttributeCollationRule {
    Binary,
    Filename,
    UnicodeString,
    __dummyEnum__0,
    __dummyEnum__1,
    __dummyEnum__2,
    __dummyEnum__3,
    __dummyEnum__4,
    __dummyEnum__5,
    __dummyEnum__6,
    __dummyEnum__7,
    __dummyEnum__8,
    __dummyEnum__9,
    __dummyEnum__10,
    __dummyEnum__11,
    __dummyEnum__12,
    UnsignedLong,
    Sid,
    SecurityHash,
    MultipleUnsignedLongs;

    public static AttributeCollationRule valueOf(int value) {
        return values()[value];
    }
}
