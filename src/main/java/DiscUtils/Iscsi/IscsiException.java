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

import dotnet4j.io.IOException;

/**
 * Base exception for any iSCSI-related failures.
 */
public class IscsiException extends IOException {
    /**
     * Initializes a new instance of the IscsiException class.
     */
    public IscsiException() {
    }

    /**
     * Initializes a new instance of the IscsiException class.
     *
     * @param message The reason for the exception.
     */
    public IscsiException(String message) {
        super(message);
    }

    /**
     * Initializes a new instance of the IscsiException class.
     *
     * @param message The reason for the exception.
     * @param innerException The inner exception.
     */
    public IscsiException(String message, Exception innerException) {
        super(message, innerException);
    }

    /**
     * Initializes a new instance of the IscsiException class.
     *
     * @param info The serialization info.
     * @param context Ther context.
     */
//    protected IscsiException(SerializationInfo info, StreamingContext context) {
//        super(info, context);
//    }
}
