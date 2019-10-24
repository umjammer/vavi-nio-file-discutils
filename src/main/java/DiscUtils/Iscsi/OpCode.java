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
//
// Initiator op-codes.
//

package DiscUtils.Iscsi;

public enum OpCode {
    NopOut,
    ScsiCommand,
    ScsiTaskManagementRequest,
    LoginRequest,
    TextRequest,
    ScsiDataOut,
    LogoutRequest,
    __dummyEnum__0,
    __dummyEnum__1,
    __dummyEnum__2,
    __dummyEnum__3,
    __dummyEnum__4,
    __dummyEnum__5,
    __dummyEnum__6,
    __dummyEnum__7,
    __dummyEnum__8,
    SnackRequest,
    __dummyEnum__9,
    __dummyEnum__10,
    __dummyEnum__11,
    __dummyEnum__12,
    __dummyEnum__13,
    __dummyEnum__14,
    __dummyEnum__15,
    __dummyEnum__16,
    __dummyEnum__17,
    __dummyEnum__18,
    __dummyEnum__19,
    __dummyEnum__20,
    __dummyEnum__21,
    __dummyEnum__22,
    __dummyEnum__23,
    //
    // Target op-codes.
    //
    NopIn,
    ScsiResponse,
    ScsiTaskManagementResponse,
    LoginResponse,
    TextResponse,
    ScsiDataIn,
    LogoutResponse,
    __dummyEnum__24,
    __dummyEnum__25,
    __dummyEnum__26,
    __dummyEnum__27,
    __dummyEnum__28,
    __dummyEnum__29,
    __dummyEnum__30,
    __dummyEnum__31,
    __dummyEnum__32,
    __dummyEnum__33,
    ReadyToTransfer,
    AsynchronousMessage,
    __dummyEnum__34,
    __dummyEnum__35,
    __dummyEnum__36,
    __dummyEnum__37,
    __dummyEnum__38,
    __dummyEnum__39,
    __dummyEnum__40,
    __dummyEnum__41,
    __dummyEnum__42,
    __dummyEnum__43,
    __dummyEnum__44,
    __dummyEnum__45,
    Reject;

    public static OpCode valueOf(int value) {
        return values()[value];
    }
}
