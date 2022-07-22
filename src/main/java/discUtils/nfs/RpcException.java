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

package discUtils.nfs;

import dotnet4j.io.IOException;

/**
 * Exception thrown when some invalid file system data is found, indicating
 * probably corruption.
 */
public final class RpcException extends IOException {
    /**
     * Initializes a new instance of the RpcException class.
     */
    public RpcException() {
    }

    /**
     * Initializes a new instance of the RpcException class.
     *
     * @param message The exception message.
     */
    public RpcException(String message) {
        super(message);
    }

    /**
     * Initializes a new instance of the RpcException class.
     *
     * @param message The exception message.
     * @param innerException The inner exception.
     */
    public RpcException(String message, Exception innerException) {
        super(message, innerException);
    }

    /**
     * Initializes a new instance of the RpcException class.
     *
     * @param reply The RPC reply from the server.
     */
    public RpcException(RpcReplyHeader reply) {
        super(generateMessage(reply));
    }

    /**
     * Initializes a new instance of the RpcException class.
     *
     * @param info The serialization info.
     * @param context The streaming context.
     */
//    private RpcException(SerializationInfo info, StreamingContext context) {
//        super(info, context);
//    }

    private static String generateMessage(RpcReplyHeader reply) {
        if (reply.Status == RpcReplyStatus.Accepted) {
            switch (reply.acceptReply.AcceptStatus) {
            case Success:
                return "RPC success";
            case ProgramUnavailable:
                return "RPC program unavailable";
            case ProgramVersionMismatch:
                if (reply.acceptReply.mismatchInfo.low == reply.acceptReply.mismatchInfo.high) {
                    return "RPC program version mismatch, server supports version " + reply.acceptReply.mismatchInfo.low;
                }

                return "RPC program version mismatch, server supports versions " + reply.acceptReply.mismatchInfo.low +
                       " through " + reply.acceptReply.mismatchInfo.high;
            case ProcedureUnavailable:
                return "RPC procedure unavailable";
            case GarbageArguments:
                return "RPC corrupt procedure arguments";
            default:
                return "RPC failure";

            }
        }

        if (reply.rejectedReply.status == RpcRejectedStatus.AuthError) {
            switch (reply.rejectedReply.authenticationStatus) {
            case BadCredentials:
                return "RPC authentication credentials bad";
            case RejectedCredentials:
                return "RPC rejected authentication credentials";
            case BadVerifier:
                return "RPC bad authentication verifier";
            case RejectedVerifier:
                return "RPC rejected authentication verifier";
            case TooWeak:
                return "RPC authentication credentials too weak";
            default:
                return "RPC authentication failure";
            }
        }

        if (reply.rejectedReply.mismatchInfo != null) {
            return String.format("RPC protocol version mismatch, server supports versions %d through %d",
                                 reply.rejectedReply.mismatchInfo.low,
                                 reply.rejectedReply.mismatchInfo.high);
        }

        return "RPC protocol version mismatch, server didn't indicate supported versions";
    }
}
