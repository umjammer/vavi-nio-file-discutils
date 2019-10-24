
package DiscUtils.Nfs;

public enum RpcAuthFlavour {
    Null,
    Unix,
    Short,
    Des;

    public static RpcAuthFlavour valueOf(int value) {
        return values()[value];
    }
}
