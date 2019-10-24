
package DiscUtils.Udf;

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
        return values()[value];
    }
}
