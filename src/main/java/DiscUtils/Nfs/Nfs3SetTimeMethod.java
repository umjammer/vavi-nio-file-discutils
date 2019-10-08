
package DiscUtils.Nfs;

import java.util.Arrays;

public enum Nfs3SetTimeMethod {
    NoChange,
    ServerTime,
    ClientTime;

    public static Nfs3SetTimeMethod valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
