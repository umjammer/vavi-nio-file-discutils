
package discUtils.nfs;

import java.io.Closeable;


public interface IRpcTransport extends Closeable {

    void send(byte[] message);

    byte[] sendAndReceive(byte[] message);

    byte[] receive();
}
