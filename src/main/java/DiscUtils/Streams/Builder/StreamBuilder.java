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

package DiscUtils.Streams.Builder;

import java.io.IOException;
import java.util.List;

import DiscUtils.Streams.BuiltStream;
import DiscUtils.Streams.SparseStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;


/**
 * Base class for objects that can dynamically construct a stream.
 */
public abstract class StreamBuilder {
    /**
     * Builds a new stream.
     *
     * @return The stream created by the StreamBuilder instance.
     */
    public SparseStream build() {
        long[] totalLength = new long[1];
        List<BuilderExtent> extents = fixExtents(totalLength);
        return new BuiltStream(totalLength[0], extents);
    }

    /**
     * Writes the stream contents to an existing stream.
     *
     * @param output The stream to write to.
     */
    public void build(Stream output) {
        try (Stream src = build()) {
            byte[] buffer = new byte[64 * 1024];
            int numRead = src.read(buffer, 0, buffer.length);
            while (numRead != 0) {
                output.write(buffer, 0, numRead);
                numRead = src.read(buffer, 0, buffer.length);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Writes the stream contents to a file.
     *
     * @param outputFile The file to write to.
     */
    public void build(String outputFile) {
        try (Stream destStream = new FileStream(outputFile, FileMode.Create, FileAccess.Write)) {
            build(destStream);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    protected abstract List<BuilderExtent> fixExtents(long[] totalLength);
}
