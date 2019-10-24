
package DiscUtils.Nfs;

public enum Nfs3SetTimeMethod {
    NoChange,
    ServerTime,
    ClientTime;

    public static Nfs3SetTimeMethod valueOf(int value) {
        return values()[value];
    }
}
