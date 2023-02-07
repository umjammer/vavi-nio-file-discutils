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

package discUtils.streams;

import java.io.Serializable;


/**
 * common interface for reading structures to/from byte arrays.
 * TODO to be deprecate, use Serdes
 */
public interface IByteArraySerializable extends Serializable {

    /**
     * Gets the total number of bytes the structure occupies.
     */
    int size();

    /**
     * Reads the structure from a byte array.
     *
     * @param buffer The buffer to read from.
     * @param offset The buffer offset to start reading from.
     * @return The number of bytes read.
     */
    int readFrom(byte[] buffer, int offset);

    /**
     * Writes a structure to a byte array.
     *
     * @param buffer The buffer to write to.
     * @param offset The buffer offset to start writing at.
     */
    void writeTo(byte[] buffer, int offset);
}
