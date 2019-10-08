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

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * RPC credentials used for accessing an access-controlled server.
 * Note there is no server-side authentication with these credentials,
 * instead the client is assumed to be trusted.
 */
public final class RpcUnixCredential extends RpcCredentials {
    /**
     * Default credentials (nobody).
     *
     * There is no standard UID/GID for nobody. This default credential
     * assumes 65534 for both the user and group.
     */
    public static final RpcUnixCredential Default = new RpcUnixCredential(65534, 65534);

    private final int _gid;

    private final int[] _gids;

    private final String _machineName;

    private final int _uid;

    /**
     * Initializes a new instance of the RpcUnixCredential class.
     *
     * @param user The user's unique id (UID).
     * @param primaryGroup The user's primary group id (GID).
     */
    public RpcUnixCredential(int user, int primaryGroup) {
        this(user, primaryGroup, new int[] {});
    }

    /**
     * Initializes a new instance of the RpcUnixCredential class.
     *
     * @param user The user's unique id (UID).
     * @param primaryGroup The user's primary group id (GID).
     * @param groups The user's supplementary group ids.
     */
    public RpcUnixCredential(int user, int primaryGroup, int[] groups) {
        try {
            _machineName = InetAddress.getLocalHost().getHostName();
            _uid = user;
            _gid = primaryGroup;
            _gids = groups;
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    public RpcAuthFlavour getAuthFlavour() {
        return RpcAuthFlavour.Unix;
    }

    public void write(XdrDataWriter writer) {
        writer.write(0);
        writer.write(_machineName);
        writer.write(_uid);
        writer.write(_gid);
        if (_gids == null) {
            writer.write(0);
        } else {
            writer.write(_gids.length);
            for (int i = 0; i < _gids.length; ++i) {
                writer.write(_gids[i]);
            }
        }
    }
}
