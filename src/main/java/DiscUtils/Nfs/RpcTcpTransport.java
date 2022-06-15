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

package DiscUtils.Nfs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.NetworkStream;
import dotnet4j.io.Stream;


public final class RpcTcpTransport implements IRpcTransport {
    private static final int RetryLimit = 20;

    private final String _address;

    private final int _localPort;

    private final int _port;

    private Socket _socket;

    private NetworkStream _tcpStream;

    public RpcTcpTransport(String address, int port) {
        this(address, port, 0);
    }

    public RpcTcpTransport(String address, int port, int localPort) {
        _address = address;
        _port = port;
        _localPort = localPort;
    }

    public void close() throws IOException {
        if (_tcpStream != null) {
            _tcpStream.close();
            _tcpStream = null;
        }

        if (_socket != null) {
            _socket.close();
            _socket = null;
        }
    }

    public byte[] sendAndReceive(byte[] message) {
        int retries = 0;
        int retryLimit = RetryLimit;
        Exception lastException = null;
        boolean isNewConnection = _socket == null;
        if (isNewConnection) {
            retryLimit = 1;
        }

        byte[] response = null;
        while (response == null && retries < retryLimit) {
            while (retries < retryLimit && (_socket == null || !_socket.isConnected())) {
                try {
                    if (_tcpStream != null) {
                        _tcpStream.close();
                        _tcpStream = null;
                    }

                    if (_socket != null) {
                        _socket.close();
                        _socket = null;
                    }

                    _socket = new Socket();
                    _socket.setReuseAddress(true);
                    _socket.setTcpNoDelay(true);
                    if (_localPort != 0) {
                        _socket.bind(new InetSocketAddress(_localPort));
                    }

                    _socket.connect(new InetSocketAddress(_address, _port));
                    _tcpStream = new NetworkStream(_socket, false);
                } catch (IOException se) {
                    retries++;
                    lastException = se;
                    if (!isNewConnection) {
                        try { Thread.sleep(1000); } catch (InterruptedException e) {}
                    }
                }
            }
            if (_tcpStream != null) {
                try {
                    send(_tcpStream, message);
                    response = receive();
                } catch (dotnet4j.io.IOException sendReceiveException) {
                    lastException = sendReceiveException;
                    try {
                        _tcpStream.close();
                        _tcpStream = null;
                        _socket.close();
                        _socket = null;
                    } catch (IOException e) {
                        throw new dotnet4j.io.IOException(e);
                    }
                }

                retries++;
            }
        }
        if (response == null) {
            throw new dotnet4j.io.IOException(String
                    .format("Unable to send RPC message to %s:%d", _address, _port), lastException);
        }

        return response;
    }

    public void send(byte[] message) {
        send(_tcpStream, message);
    }

    public static void send(Stream stream, byte[] message) {
        byte[] header = new byte[4];
        EndianUtilities.writeBytesBigEndian(0x80000000 | message.length, header, 0);
        stream.write(header, 0, 4);
        stream.write(message, 0, message.length);
        stream.flush();
    }

    public byte[] receive() {
        return receive(_tcpStream);
    }

    public static byte[] receive(Stream stream) {
        MemoryStream ms = null;
        boolean lastFragFound = false;
        while (!lastFragFound) {
            byte[] header = StreamUtilities.readExact(stream, 4);
            int headerVal = EndianUtilities.toUInt32BigEndian(header, 0);
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
