
package DiscUtils.Nfs;

import java.util.Arrays;

public enum RpcAuthFlavour {
    Null,
    Unix,
    Short,
    Des;

    public static RpcAuthFlavour valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
