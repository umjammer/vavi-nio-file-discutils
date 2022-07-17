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

package discUtils.nfs;

import java.time.ZoneId;
import java.time.ZonedDateTime;


public final class Nfs3FileTime {
    private static final long TicksPerSec = 10 * 1000 * 1000;

    // 10 million ticks per sec
    private static final long TicksPerNanoSec = 100;

    // 1 tick = 100 ns
    private static final long nfsEpoch = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();

    private final int _nseconds;

    private final int _seconds;

    public Nfs3FileTime(XdrDataReader reader) {
        _seconds = reader.readUInt32();
        _nseconds = reader.readUInt32();
    }

    public Nfs3FileTime(long time) {
        long ticks = time - nfsEpoch;
        _seconds = (int) (ticks / TicksPerSec);
        _nseconds = (int) (ticks % TicksPerSec * TicksPerNanoSec);
    }

    public Nfs3FileTime(int seconds, int nseconds) {
        _seconds = seconds;
        _nseconds = nseconds;
    }

    public long toDateTime() {
        return _seconds * TicksPerSec + _nseconds / TicksPerNanoSec + nfsEpoch;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_seconds);
        writer.write(_nseconds);
    }

    public static Nfs3FileTime getPrecision() {
        return new Nfs3FileTime(0, 1);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3FileTime ? (Nfs3FileTime) obj : null);
    }

    public boolean equals(Nfs3FileTime other) {
        if (other == null) {
            return false;
        }

        return other._seconds == _seconds && other._nseconds == _nseconds;
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(_seconds, _nseconds);
    }

    public String toString() {
        return String.valueOf(toDateTime());
    }
}
