
package DiscUtils.Iscsi;

// use ordinal()
public enum LoginStages {
    SecurityNegotiation,
    LoginOperationalNegotiation,
    __dummyEnum__0,
    FullFeaturePhase;

    public static LoginStages valueOf(int value) {
        return values()[value];
    }
}
