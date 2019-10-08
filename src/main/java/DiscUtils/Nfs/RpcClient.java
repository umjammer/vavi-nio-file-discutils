//
// Copyright (c) 2008-2011, Kenneth Bell
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
import java.util.Map;
import java.util.Random;


public final class RpcClient implements IRpcClient {
    private int _nextTransaction;

    private final String _serverAddress;

    private Map<Integer, RpcTcpTransport> _transports;

    public RpcClient(String address, RpcCredentials credential) {
        _serverAddress = address;
        __Credentials = credential;
        _nextTransaction = new Random().nextInt();
        _transports.put(PortMap2.ProgramIdentifier, new RpcTcpTransport(address, 111));
    }

    private RpcCredentials __Credentials;

    public RpcCredentials getCredentials() {
        return __Credentials;
    }

    public void close() throws IOException {
        if (_transports != null) {
            for (RpcTcpTransport transport : _transports.values()) {
                transport.close();
            }
            _transports = null;
        }
    }

    public int nextTransactionId() {
        return _nextTransaction++;
    }

    public IRpcTransport getTransport(int program, int version) {
        RpcTcpTransport transport;
        if (!_transports.containsKey(program)) {
            PortMap2 pm = new PortMap2(this);
            int port = pm.getPort(program, version, PortMap2Protocol.Tcp);
            transport = new RpcTcpTransport(_serverAddress, port);
            _transports.put(program, transport);
        }
        transport = _transports.get(program);

        return transport;
    }
}
