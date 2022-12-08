//
// Copyright (c) 2008-2012, Kenneth Bell
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

import java.util.Arrays;

import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;


/**
 * Aligns I/O to a given block size.
 * Uses the read-modify-write pattern to align I/O.
 */
public final class AligningStream extends WrappingMappedStream<SparseStream> {

    private final byte[] alignmentBuffer;

    private final int blockSize;

    private long position;

    public AligningStream(SparseStream toWrap, Ownership ownership, int blockSize) {
        super(toWrap, ownership, null);
        this.blockSize = blockSize;
        alignmentBuffer = new byte[blockSize];
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
    }

    public int read(byte[] buffer, int offset, int count) {
        int startOffset = (int) (position % blockSize);
        if (startOffset == 0 && (count % blockSize == 0 || position + count == getLength())) {
            // Aligned read - pass through to underlying stream.
            getWrappedStream().position(position);
            int numRead = getWrappedStream().read(buffer, offset, count);
            position += numRead;
            return numRead;
        }

        long startPos = MathUtilities.roundDown(position, blockSize);
        long endPos = MathUtilities.roundUp(position + count, blockSize);

        if (endPos - startPos > Integer.MAX_VALUE) {
            throw new dotnet4j.io.IOException("Oversized read, after alignment");
        }

        byte[] tempBuffer = new byte[(int) (endPos - startPos)];

        getWrappedStream().position(startPos);
        int read = getWrappedStream().read(tempBuffer, 0, tempBuffer.length);
        int available = Math.min(count, read - startOffset);

        System.arraycopy(tempBuffer, startOffset, buffer, offset, available);

        position += available;
        return available;
    }

    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of stream");
        }
        position = effectiveOffset;
        return position;
    }

    public void clear(int count) {
        doOperation((s, opOffset, opCount) -> s.clear(opCount),
                (buffer, offset, opOffset, opCount) -> Arrays.fill(buffer, offset, offset + opCount, (byte) 0), count);
    }

    public void write(byte[] buffer, int offset, int count) {
        doOperation((s, opOffset, opCount) -> s.write(buffer, offset + opOffset, opCount),
                (tempBuffer, tempOffset, opOffset, opCount) -> System.arraycopy(buffer, offset + opOffset, tempBuffer, tempOffset, opCount), count);
    }

    private void doOperation(ModifyStream modifyStream, ModifyBuffer modifyBuffer, int count) {
        int startOffset = (int) (position % blockSize);
        if (startOffset == 0 && (count % blockSize == 0 || position + count == getLength())) {
            getWrappedStream().position(position);
            modifyStream.invoke(getWrappedStream(), 0, count);
            position += count;
            return;
        }

        long unalignedEnd = position + count;
        long alignedPos = MathUtilities.roundDown(position, blockSize);

        if (startOffset != 0) {
            getWrappedStream().position(alignedPos);
            getWrappedStream().read(alignmentBuffer, 0, blockSize);

            modifyBuffer.invoke(alignmentBuffer, startOffset, 0, Math.min(count, blockSize - startOffset));

            getWrappedStream().position(alignedPos);
            getWrappedStream().write(alignmentBuffer, 0, blockSize);
        }

        alignedPos = MathUtilities.roundUp(position, blockSize);
        if (alignedPos >= unalignedEnd) {
            position = unalignedEnd;
            return;
        }

        int passthroughLength = (int) MathUtilities.roundDown(position + count - alignedPos, blockSize);
        if (passthroughLength > 0) {
            getWrappedStream().position(alignedPos);
            modifyStream.invoke(getWrappedStream(), (int) (alignedPos - position), passthroughLength);
        }

        alignedPos += passthroughLength;
        if (alignedPos >= unalignedEnd) {
            position = unalignedEnd;
            return;
        }

        getWrappedStream().position(alignedPos);
        getWrappedStream().read(alignmentBuffer, 0, blockSize);

        modifyBuffer.invoke(alignmentBuffer,
                            0,
                            (int) (alignedPos - position),
                            (int) Math.min(count - (alignedPos - position), unalignedEnd - alignedPos));

        getWrappedStream().position(alignedPos);
        getWrappedStream().write(alignmentBuffer, 0, blockSize);

        position = unalignedEnd;
    }

    @FunctionalInterface
    private interface ModifyStream {

        void invoke(SparseStream stream, int opOffset, int count);
    }

    @FunctionalInterface
    private interface ModifyBuffer {

        void invoke(byte[] buffer, int offset, int opOffset, int count);
    }
}
