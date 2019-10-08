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

package DiscUtils.Streams;

import java.util.Arrays;

import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


/**
 * Aligns I/O to a given block size.
 * Uses the read-modify-write pattern to align I/O.
 */
public final class AligningStream extends WrappingMappedStream<SparseStream> {
    private final byte[] _alignmentBuffer;

    private final int _blockSize;

    private long _position;

    public AligningStream(SparseStream toWrap, Ownership ownership, int blockSize) {
        super(toWrap, ownership, null);
        _blockSize = blockSize;
        _alignmentBuffer = new byte[blockSize];
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
    }

    public int read(byte[] buffer, int offset, int count) {
        int startOffset = (int) (_position % _blockSize);
        if (startOffset == 0 && (count % _blockSize == 0 || _position + count == getLength())) {
            // Aligned read - pass through to underlying stream.
            getWrappedStream().setPosition(_position);
            int numRead = getWrappedStream().read(buffer, offset, count);
            _position += numRead;
            return numRead;
        }

        long startPos = MathUtilities.roundDown(_position, _blockSize);
        long endPos = MathUtilities.roundUp(_position + count, _blockSize);
        if (endPos - startPos > Integer.MAX_VALUE) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Oversized read, after alignment");
        }

        byte[] tempBuffer = new byte[(int) (endPos - startPos)];
        getWrappedStream().setPosition(startPos);
        int read = getWrappedStream().read(tempBuffer, 0, tempBuffer.length);
        int available = Math.min(count, read - startOffset);
        System.arraycopy(tempBuffer, startOffset, buffer, offset, available);
        _position += available;
        return available;
    }

    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        if (effectiveOffset < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of stream");
        }

        _position = effectiveOffset;
        return _position;
    }

    public void clear(int count) {
        doOperation((s, opOffset, opCount) -> {
            s.clear(opCount);
        }, (buffer, offset, opOffset, opCount) -> {
            Arrays.fill(buffer, offset, opCount, (byte) 0);
        }, count);
    }

    public void write(byte[] buffer, int offset, int count) {
        doOperation((s, opOffset, opCount) -> {
            s.write(buffer, offset + opOffset, opCount);
        }, (tempBuffer, tempOffset, opOffset, opCount) -> {
            System.arraycopy(buffer, offset + opOffset, tempBuffer, tempOffset, opCount);
        }, count);
    }

    private void doOperation(ModifyStream modifyStream, ModifyBuffer modifyBuffer, int count) {
        int startOffset = (int) (_position % _blockSize);
        if (startOffset == 0 && (count % _blockSize == 0 || _position + count == getLength())) {
            getWrappedStream().setPosition(_position);
            modifyStream.invoke(getWrappedStream(), 0, count);
            _position += count;
            return;
        }

        long unalignedEnd = _position + count;
        long alignedPos = MathUtilities.roundDown(_position, _blockSize);
        if (startOffset != 0) {
            getWrappedStream().setPosition(alignedPos);
            getWrappedStream().read(_alignmentBuffer, 0, _blockSize);
            modifyBuffer.invoke(_alignmentBuffer, startOffset, 0, Math.min(count, _blockSize - startOffset));
            getWrappedStream().setPosition(alignedPos);
            getWrappedStream().write(_alignmentBuffer, 0, _blockSize);
        }

        alignedPos = MathUtilities.roundUp(_position, _blockSize);
        if (alignedPos >= unalignedEnd) {
            _position = unalignedEnd;
            return;
        }

        int passthroughLength = (int) MathUtilities.roundDown(_position + count - alignedPos, _blockSize);
        if (passthroughLength > 0) {
            getWrappedStream().setPosition(alignedPos);
            modifyStream.invoke(getWrappedStream(), (int) (alignedPos - _position), passthroughLength);
        }

        alignedPos += passthroughLength;
        if (alignedPos >= unalignedEnd) {
            _position = unalignedEnd;
            return;
        }

        getWrappedStream().setPosition(alignedPos);
        getWrappedStream().read(_alignmentBuffer, 0, _blockSize);
        modifyBuffer.invoke(_alignmentBuffer,
                            0,
                            (int) (alignedPos - _position),
                            (int) Math.min(count - (alignedPos - _position), unalignedEnd - alignedPos));
        getWrappedStream().setPosition(alignedPos);
        getWrappedStream().write(_alignmentBuffer, 0, _blockSize);
        _position = unalignedEnd;
    }

//    private static class __MultiModifyStream implements ModifyStream {
//        public void invoke(SparseStream stream, int opOffset, int count) {
//            List<ModifyStream> copy = new ArrayList<>(), members = this.getInvocationList();
//            synchronized (members) {
//                copy = new LinkedList<>(members);
//            }
//            for (ModifyStream d : copy) {
//                d.invoke(stream, opOffset, count);
//            }
//        }
//
//        private List<ModifyStream> _invocationList = new ArrayList<>();
//
//        public static ModifyStream combine(ModifyStream a, ModifyStream b) {
//            if (a == null)
//                return b;
//
//            if (b == null)
//                return a;
//
//            __MultiModifyStream ret = new __MultiModifyStream();
//            ret._invocationList = a.getInvocationList();
//            ret._invocationList.addAll(b.getInvocationList());
//            return ret;
//        }
//
//        public static ModifyStream remove(ModifyStream a, ModifyStream b) {
//            if (a == null || b == null)
//                return a;
//
//            List<ModifyStream> aInvList = a.getInvocationList();
//            List<ModifyStream> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//            if (aInvList == newInvList) {
//                return a;
//            } else {
//                __MultiModifyStream ret = new __MultiModifyStream();
//                ret._invocationList = newInvList;
//                return ret;
//            }
//        }
//
//        public List<ModifyStream> getInvocationList() {
//            return _invocationList;
//        }
//
//    }

    @FunctionalInterface
    private static interface ModifyStream {
        void invoke(SparseStream stream, int opOffset, int count);

//        List<ModifyStream> getInvocationList();

    }

//    private static class __MultiModifyBuffer implements ModifyBuffer {
//        public void invoke(byte[] buffer, int offset, int opOffset, int count) {
//            List<ModifyBuffer> copy = new ArrayList<>(), members = this.getInvocationList();
//            synchronized (members) {
//                copy = new LinkedList<>(members);
//            }
//            for (ModifyBuffer d : copy) {
//                d.invoke(buffer, offset, opOffset, count);
//            }
//        }
//
//        private List<ModifyBuffer> _invocationList = new ArrayList<>();
//
//        public static ModifyBuffer combine(ModifyBuffer a, ModifyBuffer b) {
//            if (a == null)
//                return b;
//
//            if (b == null)
//                return a;
//
//            __MultiModifyBuffer ret = new __MultiModifyBuffer();
//            ret._invocationList = a.getInvocationList();
//            ret._invocationList.addAll(b.getInvocationList());
//            return ret;
//        }
//
//        public static ModifyBuffer remove(ModifyBuffer a, ModifyBuffer b) {
//            if (a == null || b == null)
//                return a;
//
//            List<ModifyBuffer> aInvList = a.getInvocationList();
//            List<ModifyBuffer> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//            if (aInvList == newInvList) {
//                return a;
//            } else {
//                __MultiModifyBuffer ret = new __MultiModifyBuffer();
//                ret._invocationList = newInvList;
//                return ret;
//            }
//        }
//
//        public List<ModifyBuffer> getInvocationList() {
//            return _invocationList;
//        }
//
//    }

    @FunctionalInterface
    private static interface ModifyBuffer {
        void invoke(byte[] buffer, int offset, int opOffset, int count);

//        List<ModifyBuffer> getInvocationList();

    }

}
