
package DiscUtils.Iscsi;

import java.util.Arrays;

public enum LoginStages {
    SecurityNegotiation,
    LoginOperationalNegotiation,
    __dummyEnum__0,
    FullFeaturePhase;

    public static LoginStages valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
