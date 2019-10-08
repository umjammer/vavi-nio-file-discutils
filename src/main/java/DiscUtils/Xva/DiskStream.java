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

package DiscUtils.Xva;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import DiscUtils.Core.Archives.FileRecord;
import DiscUtils.Core.Archives.TarFile;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.ZeroStream;
import DiscUtils.Streams.Util.Sizes;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


public class DiskStream extends SparseStream {
    private final TarFile _archive;

    private final String _dir;

    private final long _length;

    private Stream _currentChunkData;

    private int _currentChunkIndex;

    private long _position;

    private List<Integer> _skipChunks;

    public DiskStream(TarFile archive, long length, String dir) {
        _archive = archive;
        _length = length;
        _dir = dir;
        if (!archive.dirExists(_dir)) {
            throw new moe.yo3explorer.dotnetio4j.IOException("No such disk");
        }

        readChunkSkipList();
    }

    public boolean canRead() {
        return true;
    }

    public boolean canSeek() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public List<StreamExtent> getExtents() {
        List<StreamExtent> extents = new ArrayList<>();
        long chunkSize = Sizes.OneMiB;
        int i = 0;
        int numChunks = (int) ((_length + chunkSize - 1) / chunkSize);
        while (i < numChunks) {
            while (i < numChunks && !chunkExists(i)) {
                // Find next stored block
                ++i;
            }
            int start = i;
            while (i < numChunks && chunkExists(i)) {
                // Find next absent block
                ++i;
            }
            if (start != i) {
                extents.add(new StreamExtent(start * chunkSize, (i - start) * chunkSize));
            }

        }
        return extents;
    }

    public long getLength() {
        return _length;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        if (value > _length) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move beyond end of stream");
        }

        _position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (_position == _length) {
            return 0;
        }

        if (_position > _length) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to read beyond end of stream");
        }

        int chunk = correctChunkIndex((int) (_position / Sizes.OneMiB));
        if (_currentChunkIndex != chunk || _currentChunkData == null) {
            if (_currentChunkData != null) {
                try {
                    _currentChunkData.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
                _currentChunkData = null;
            }

            Stream[] tmp = new Stream[1];
            boolean result = !_archive.tryOpenFile(String.format("{0}/{1:D8}", _dir, chunk), tmp);
            _currentChunkData = tmp[0];
            if (result) {
                _currentChunkData = new ZeroStream(Sizes.OneMiB);
            }

            _currentChunkIndex = chunk;
        }

        long chunkOffset = _position % Sizes.OneMiB;
        int toRead = Math.min((int) Math.min(Sizes.OneMiB - chunkOffset, _length - _position), count);
        _currentChunkData.setPosition(chunkOffset);
        int numRead = _currentChunkData.read(buffer, offset, toRead);
        _position += numRead;
        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += _length;
        }

        if (effectiveOffset < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of disk");
        }

        setPosition(effectiveOffset);
        return getPosition();
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    private boolean chunkExists(int i) {
        return _archive.fileExists(String.format("{0}/{1:D8}", _dir, correctChunkIndex(i)));
    }

    private void readChunkSkipList() {
        List<Integer> skipChunks = new ArrayList<>();
        for (FileRecord fileInfo : _archive.getFiles(_dir)) {
            if (fileInfo.Length == 0) {
                String path = fileInfo.Name.replace('/', '\\');
                try {
                    int index = Integer.parseInt(Utilities.getFileFromPath(path));
                    skipChunks.add(index);
                } catch (NumberFormatException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
        Collections.sort(skipChunks);
        _skipChunks = skipChunks;
    }

    private int correctChunkIndex(int rawIndex) {
        int index = rawIndex;
        for (int i = 0; i < _skipChunks.size(); ++i) {
            if (index >= _skipChunks.get(i)) {
                ++index;
            } else if (index < +_skipChunks.get(i)) {
                break;
            }
        }
        return index;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
    }
}
