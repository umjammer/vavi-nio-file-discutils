
package discUtils.nfs;

import java.util.Arrays;
import java.util.Optional;


/**
 * An RPC authentication flavour, as sent on the wire.
 * <p>
 * The numbers are not consecutive: besides the flavours of RFC 5531 a server
 * may name the RPCSEC_GSS pseudo flavours of RFC 2623, which is what an NFS
 * server does when it lists what a mount point accepts.
 */
public enum RpcAuthFlavour {
    Null(0),
    Unix(1),
    Short(2),
    Des(3),
    RpcSecGss(6),
    /** kerberos v5, authentication only */
    Krb5(390003),
    /** kerberos v5 with integrity protection */
    Krb5i(390004),
    /** kerberos v5 with privacy protection */
    Krb5p(390005);

    private final int value;

    RpcAuthFlavour(int value) {
        this.value = value;
    }

    /** the number of this flavour on the wire */
    public int getValue() {
        return value;
    }

    /**
     * @return the flavour {@code value} stands for, or empty for one this
     *         implementation has no name for
     */
    public static Optional<RpcAuthFlavour> of(int value) {
        return Arrays.stream(values()).filter(flavour -> flavour.value == value).findFirst();
    }
}
