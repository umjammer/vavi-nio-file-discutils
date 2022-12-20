//
// Copyright (c) 2008-2011, Kenneth Bell
// Copyright (c) 2017, Quamotion
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.nfs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.NetworkStream;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public final class RpcTcpTransport implements IRpcTransport {

    private static final int RetryLimit = 20;

    private final String address;

    private final int localPort;

    private final int port;

    private Socket socket;

    private NetworkStream tcpStream;

    public RpcTcpTransport(String address, int port) {
        this(address, port, 0);
    }

    public RpcTcpTransport(String address, int port, int localPort) {
        this.address = address;
        this.port = port;
        this.localPort = localPort;
    }

    @Override
    public void close() throws IOException {
        if (tcpStream != null) {
            tcpStream.close();
            tcpStream = null;
        }

        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public byte[] sendAndReceive(byte[] message) {
        int retries = 0;
        int retryLimit = RetryLimit;
        Exception lastException = null;
        boolean isNewConnection = socket == null;
        if (isNewConnection) {
            retryLimit = 1;
        }

        byte[] response = null;
        while (response == null && retries < retryLimit) {
            while (retries < retryLimit && (socket == null || !socket.isConnected())) {
                try {
                    if (tcpStream != null) {
                        tcpStream.close();
                        tcpStream = null;
                    }

                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }

                    socket = new Socket();
                    socket.setReuseAddress(true);
                    socket.setTcpNoDelay(true);
                    if (localPort != 0) {
                        socket.bind(new InetSocketAddress(localPort));
                    }

                    socket.connect(new InetSocketAddress(address, port));
                    tcpStream = new NetworkStream(socket, false);
                } catch (IOException se) {
                    retries++;
                    lastException = se;
                    if (!isNewConnection) {
                        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    }
                }
            }
            if (tcpStream != null) {
                try {
                    send(tcpStream, message);
                    response = receive();
                } catch (dotnet4j.io.IOException sendReceiveException) {
                    lastException = sendReceiveException;
                    try {
                        tcpStream.close();
                        tcpStream = null;
                        socket.close();
                        socket = null;
                    } catch (IOException e) {
                        throw new dotnet4j.io.IOException(e);
                    }
                }

                retries++;
            }
        }
        if (response == null) {
            throw new dotnet4j.io.IOException(String
                    .format("Unable to send RPC message to %s:%d", address, port), lastException);
        }

        return response;
    }

    @Override
    public void send(byte[] message) {
        send(tcpStream, message);
    }

    public static void send(Stream stream, byte[] message) {
        byte[] header = new byte[4];
        ByteUtil.writeBeInt(0x80000000 | message.length, header, 0);
        stream.write(header, 0, 4);
        stream.write(message, 0, message.length);
        stream.flush();
    }

    @Override
    public byte[] receive() {
        return receive(tcpStream);
    }

    public static byte[] receive(Stream stream) {
        MemoryStream ms = null;
        boolean lastFragFound = false;
        while (!lastFragFound) {
            byte[] header = StreamUtilities.readExact(stream, 4);
            int headerVal = ByteUtil.readBeInt(header, 0);
            lastFragFound = (headerVal & 0x80000000) != 0;
            byte[] frag = StreamUtilities.readExact(stream, headerVal & 0x7FFFFFFF);
            if (ms != null) {
                ms.write(frag, 0, frag.length);
            } else if (!lastFragFound) {
                ms = new MemoryStream();
                ms.write(frag, 0, frag.length);
            } else {
                return frag;
            }
        }
        return ms.toArray();
    }
}
