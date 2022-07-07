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

package discUtils.iso9660;

import java.nio.charset.StandardCharsets;

import discUtils.streams.builder.BuilderExtent;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class FileExtent extends BuilderExtent {
    private final BuildFileInfo _fileInfo;

    private Stream _readStream;

    public FileExtent(BuildFileInfo fileInfo, long start) {
        super(start, fileInfo.getDataSize(StandardCharsets.US_ASCII));
        _fileInfo = fileInfo;
    }

    public void close() throws IOException {
        if (_readStream != null) {
            _fileInfo.closeStream(_readStream);
            _readStream = null;
        }

    }

    public void prepareForRead() {
        _readStream = _fileInfo.openStream();
    }

    public int read(long diskOffset, byte[] block, int offset, int count) {
        long relPos = diskOffset - getStart();
        int totalRead = 0;
        // Don't arbitrarily set position, just in case stream implementation is
        // non-seeking, and we're doing sequential reads
        if (_readStream.getPosition() != relPos) {
            _readStream.setPosition(relPos);
        }

        // Read up to EOF
        int numRead = _readStream.read(block, offset, count);
        totalRead += numRead;
        while (numRead > 0 && totalRead < count) {
            numRead = _readStream.read(block, offset + totalRead, count - totalRead);
            totalRead += numRead;
        }
        return totalRead;
    }

    public void disposeReadState() {
        _fileInfo.closeStream(_readStream);
        _readStream = null;
    }
}
