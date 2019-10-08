
package DiscUtils.Nfs;

import java.io.Closeable;


public interface IRpcClient extends Closeable {
    RpcCredentials getCredentials();

    IRpcTransport getTransport(int program, int version);

    int nextTransactionId();
}
