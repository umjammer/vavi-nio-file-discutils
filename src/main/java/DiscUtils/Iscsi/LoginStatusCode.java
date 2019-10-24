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

package DiscUtils.Iscsi;

public enum LoginStatusCode {
    /**
     * Reasons for iSCSI login sucess or failure.
     *
     * Login succeeded.
     */
    Success,
    /**
     * The iSCSI target name has moved temporarily to a new address.
     */
    TargetMovedTemporarily,
    /**
     * The iSCSI target name has moved permanently to a new address.
     */
    TargetMovedPermanently,
    /**
     * The Initiator was at fault.
     */
    InitiatorError,
    /**
     * The Initiator could not be authenticated, or the Target doesn't support
     * authentication.
     */
    AuthenticationFailure,
    /**
     * The Initiator is not permitted to access the given Target.
     */
    AuthorizationFailure,
    /**
     * The given iSCSI Target Name was not found.
     */
    NotFound,
    /**
     * The Target has been removed, and no new address provided.
     */
    TargetRemoved,
    /**
     * The Target does not support this version of the iSCSI protocol.
     */
    UnsupportedVersion,
    /**
     * Too many connections for this iSCSI session.
     */
    TooManyConnections,
    /**
     * A required parameter is missing.
     */
    MissingParameter,
    /**
     * The Target does not support session spanning to this connection
     * (address).
     */
    CannotIncludeInSession,
    /**
     * The Target does not support this type of session (or not from this
     * Initiator).
     */
    SessionTypeNotSupported,
    /**
     * Attempt to add a connection to a non-existent session.
     */
    SessionDoesNotExist,
    /**
     * An invalid request was sent during the login sequence.
     */
    InvalidDuringLogin,
    /**
     * The Target suffered an unknown hardware or software failure.
     */
    TargetError,
    /**
     * The iSCSI service or Target is not currently operational.
     */
    ServiceUnavailable,
    /**
     * The Target is out of resources.
     */
    OutOfResources;

    public static LoginStatusCode valueOf(int value) {
        return values()[value];
    }
}
