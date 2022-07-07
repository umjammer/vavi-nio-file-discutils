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

import java.util.Arrays;

/**
 * Reasons for iSCSI login sucess or failure.
 */
public enum LoginStatusCode {
    /**
     * Login succeeded.
     */
    Success(0),
    /**
     * The iSCSI target name has moved temporarily to a new address.
     */
    TargetMovedTemporarily(0x0101),
    /**
     * The iSCSI target name has moved permanently to a new address.
     */
    TargetMovedPermanently(0x0102),
    /**
     * The Initiator was at fault.
     */
    InitiatorError(0x0200),
    /**
     * The Initiator could not be authenticated, or the Target doesn't support
     * authentication.
     */
    AuthenticationFailure(0x0201),
    /**
     * The Initiator is not permitted to access the given Target.
     */
    AuthorizationFailure(0x0202),
    /**
     * The given iSCSI Target Name was not found.
     */
    NotFound(0x0203),
    /**
     * The Target has been removed, and no new address provided.
     */
    TargetRemoved(0x0204),
    /**
     * The Target does not support this version of the iSCSI protocol.
     */
    UnsupportedVersion(0x0205),
    /**
     * Too many connections for this iSCSI session.
     */
    TooManyConnections(0x0206),
    /**
     * A required parameter is missing.
     */
    MissingParameter(0x0207),
    /**
     * The Target does not support session spanning to this connection
     * (address).
     */
    CannotIncludeInSession(0x0208),
    /**
     * The Target does not support this type of session (or not from this
     * Initiator).
     */
    SessionTypeNotSupported(0x0209),
    /**
     * Attempt to add a connection to a non-existent session.
     */
    SessionDoesNotExist(0x020A),
    /**
     * An invalid request was sent during the login sequence.
     */
    InvalidDuringLogin(0x020B),
    /**
     * The Target suffered an unknown hardware or software failure.
     */
    TargetError(0x0300),
    /**
     * The iSCSI service or Target is not currently operational.
     */
    ServiceUnavailable(0x0301),
    /**
     * The Target is out of resources.
     */
    OutOfResources(0x0302);

    private final int value;

    public int getValue() {
        return value;
    }

    LoginStatusCode(int value) {
        this.value = value;
    }

    public static LoginStatusCode valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().get();
    }
}
