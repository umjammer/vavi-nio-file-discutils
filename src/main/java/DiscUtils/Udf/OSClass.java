
package DiscUtils.Udf;

import java.util.Arrays;

public enum OSClass {
    None,
    Dos,
    OS2,
    Macintosh,
    Unix,
    Windows9x,
    WindowsNt,
    Os400,
    BeOS,
    WindowsCe;

    public static OSClass valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
