//
// Copyright (c) 2008-2013, Kenneth Bell
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

package DiscUtils.Vhdx;

import java.util.ArrayList;


final class LogSequence extends ArrayList<LogEntry> {
    public LogEntry getHead() {
        return size() > 0 ? get(size() - 1) : null;
    }

    public LogEntry getTail() {
        return size() > 0 ? get(0) : null;
    }

    public boolean contains(long position) {
        if (size() <= 0) {
            return false;
        }

        if (getHead().getPosition() >= getTail().getPosition()) {
            return position >= getTail().getPosition() && position < getHead().getPosition() + LogEntry.LogSectorSize;
        }

        return position >= getTail().getPosition() || position < getHead().getPosition() + LogEntry.LogSectorSize;
    }

    boolean higherSequenceThan(LogSequence otherSequence) {
        long other = otherSequence.size() > 0 ? otherSequence.getHead().getSequenceNumber() : 0;
        long self = size() > 0 ? getHead().getSequenceNumber() : 0;
        return self > other;
    }
}
