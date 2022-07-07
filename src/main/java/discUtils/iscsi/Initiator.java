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

package discUtils.iscsi;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Class representing an iSCSI initiator. Normally, this is the first class
 * instantiated when talking to an iSCSI Portal (i.e. network entity). Create an
 * instance and configure it, before communicating with the Target.
 */
public class Initiator {
    @SuppressWarnings("unused")
    private static final int DefaultPort = 3260;

    private String _password;

    private String _userName;

    /**
     * Sets credentials used to authenticate this Initiator to the Target.
     *
     * @param userName The user name.
     * @param password The password, should be at least 12 characters.
     */
    public void setCredentials(String userName, String password) {
        _userName = userName;
        _password = password;
    }

    /**
     * Connects to a Target.
     *
     * @param target The Target to connect to.
     * @return The session representing the target connection.
     */
    public Session connectTo(TargetInfo target) {
        return connectTo(target.getName(), target.getAddresses());
    }

    /**
     * Connects to a Target.
     *
     * @param target The Target to connect to.
     * @param addresses The list of addresses for the target.
     * @return The session representing the target connection.
     */
    public Session connectTo(String target, String... addresses) {
        TargetAddress[] addressObjs = new TargetAddress[addresses.length];
        for (int i = 0; i < addresses.length; ++i) {
            addressObjs[i] = TargetAddress.parse(addresses[i]);
        }
        return connectTo(target, Arrays.asList(addressObjs));
    }

    /**
     * Connects to a Target.
     *
     * @param target The Target to connect to.
     * @param addresses The list of addresses for the target.
     * @return The session representing the target connection.
     */
    public Session connectTo(String target, List<TargetAddress> addresses) {
        return new Session(SessionType.Normal, target, _userName, _password, addresses);
    }

    /**
     * Gets the Targets available from a Portal (i.e. network entity).
     *
     * @param address The address of the Portal.
     * @return The list of Targets available.If you just have an IP address, use
     *         this method to discover the available Targets.
     */
    public TargetInfo[] getTargets(String address) {
        return getTargets(TargetAddress.parse(address));
    }

    /**
     * Gets the Targets available from a Portal (i.e. network entity).
     *
     * @param address The address of the Portal.
     * @return The list of Targets available.If you just have an IP address, use
     *         this method to discover the available Targets.
     */
    public TargetInfo[] getTargets(TargetAddress address) {
        try (Session session = new Session(SessionType.Discovery, null, _userName, _password, Collections.singletonList(address))) {
            return session.enumerateTargets();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }
}
