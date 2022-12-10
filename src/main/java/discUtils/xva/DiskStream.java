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

package discUtils.xva;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import discUtils.core.archives.FileRecord;
import discUtils.core.archives.TarFile;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.ZeroStream;
import discUtils.streams.util.Sizes;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class DiskStream extends SparseStream {

    private static final Logger logger = Logger.getLogger(DiskStream.class.getName());

    private final TarFile archive;

    private final String dir;

    private final long length;

    private Stream currentChunkData;

    private int currentChunkIndex;

    private long position;

    private List<Integer> skipChunks;

    public DiskStream(TarFile archive, long length, String dir) {
        this.archive = archive;
        this.length = length;
        this.dir = dir;
        if (!archive.dirExists(this.dir)) {
            throw new dotnet4j.io.IOException("No such disk");
        }

        readChunkSkipList();
    }

    @Override public boolean canRead() {
        return true;
    }

    @Override public boolean canSeek() {
        return true;
    }

    @Override public boolean canWrite() {
        return false;
    }

    @Override public List<StreamExtent> getExtents() {
        List<StreamExtent> extents = new ArrayList<>();

        long chunkSize = Sizes.OneMiB;
        int i = 0;
        int numChunks = (int) ((length + chunkSize - 1) / chunkSize);
        while (i < numChunks) {
            // Find next stored block
            while (i < numChunks && !chunkExists(i)) {
                ++i;
            }

            int start = i;

            // Find next absent block
            while (i < numChunks && chunkExists(i)) {
                ++i;
            }

            if (start != i) {
                extents.add(new StreamExtent(start * chunkSize, (i - start) * chunkSize));
            }
        }

        return extents;
    }

    @Override public long getLength() {
        return length;
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        if (value > length) {
            throw new dotnet4j.io.IOException("Attempt to move beyond end of stream");
        }

        position = value;
    }

    @Override public void flush() {
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        if (position == length) {
            return 0;
        }

        if (position > length) {
            throw new dotnet4j.io.IOException("Attempt to read beyond end of stream");
        }

        int chunk = correctChunkIndex((int) (position / Sizes.OneMiB));
        if (currentChunkIndex != chunk || currentChunkData == null) {
            if (currentChunkData != null) {
                try {
                    currentChunkData.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
                currentChunkData = null;
            }

            Stream[] tmp = new Stream[1];
            boolean result = !archive.tryOpenFile(String.format("%s/%8d", dir, chunk), tmp);
            currentChunkData = tmp[0];
            if (result) {
                currentChunkData = new ZeroStream(Sizes.OneMiB);
            }

            currentChunkIndex = chunk;
        }

        long chunkOffset = position % Sizes.OneMiB;
        int toRead = Math.min((int) Math.min(Sizes.OneMiB - chunkOffset, length - position), count);
        currentChunkData.position(chunkOffset);
        int numRead = currentChunkData.read(buffer, offset, toRead);
        position += numRead;
        return numRead;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += length;
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }

        position(effectiveOffset);
        return position();
    }

    @Override public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    private boolean chunkExists(int i) {
        return archive.fileExists(String.format("%s/%8d", dir, correctChunkIndex(i)));
    }

    private void readChunkSkipList() {
        List<Integer> skipChunks = new ArrayList<>();
        for (FileRecord fileInfo : archive.getFiles(dir)) {
            if (fileInfo.length == 0) {
                String path = fileInfo.name.replace('/', File.separatorChar);
                try {
                    int index = Integer.parseInt(Utilities.getFileFromPath(path));
                    skipChunks.add(index);
                } catch (NumberFormatException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
        Collections.sort(skipChunks);
        this.skipChunks = skipChunks;
    }

    private int correctChunkIndex(int rawIndex) {
        int index = rawIndex;
        for (Integer skipChunk : skipChunks) {
            if (index >= skipChunk) {
                ++index;
            } else if (index < skipChunk) {
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
