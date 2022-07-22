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

package discUtils.iso9660.rockRidge;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import discUtils.iso9660.IsoUtilities;
import discUtils.iso9660.susp.SystemUseEntry;


public final class FileTimeSystemUseEntry extends SystemUseEntry {
    public enum Timestamps {
//        None,
        Creation,
        Modify,
        Access,
        Attributes,
        Backup,
        Expiration,
        Effective;

        private final int value = 1 << ordinal();

        public Supplier<Integer> supplier() {
            return () -> value;
        }

        public Function<Integer, Boolean> function() {
            return v -> (v & value) != 0;
        }

        public static EnumSet<Timestamps> valueOf(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.function().apply(value))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Timestamps.class)));
        }
    }

    public long accessTime;

    public long attributesTime;

    public long backupTime;

    public long creationTime;

    public long effectiveTime;

    public long expirationTime;

    public long modifyTime;

    public EnumSet<Timestamps> timestampsPresent = EnumSet.noneOf(Timestamps.class);

    public FileTimeSystemUseEntry(String name, byte length, byte version, byte[] data, int offset) {
        checkAndSetCommonProperties(name, length, version, (byte) 5, (byte) 1);
        byte flags = data[offset + 4];
        boolean longForm = (flags & 0x80) != 0;
        @SuppressWarnings("unused")
        int fieldLen = longForm ? 17 : 7;
        timestampsPresent = Timestamps.valueOf(flags & 0x7F);
        int[] pos = new int[] {
            offset + 5
        };
        creationTime = readTimestamp(Timestamps.Creation, data, longForm, pos);
        modifyTime = readTimestamp(Timestamps.Modify, data, longForm, pos);
        accessTime = readTimestamp(Timestamps.Access, data, longForm, pos);
        attributesTime = readTimestamp(Timestamps.Attributes, data, longForm, pos);
        backupTime = readTimestamp(Timestamps.Backup, data, longForm, pos);
        expirationTime = readTimestamp(Timestamps.Expiration, data, longForm, pos);
        effectiveTime = readTimestamp(Timestamps.Effective, data, longForm, pos);
    }

    /**
     * @param pos {@cs out}
     */
    private long readTimestamp(Timestamps timestamp, byte[] data, boolean longForm, int[] pos) {
        long result = Long.MIN_VALUE;
        if (timestampsPresent.contains(timestamp)) {
            if (longForm) {
                result = IsoUtilities.toDateTimeFromVolumeDescriptorTime(data, pos[0]);
                pos[0] = pos[0] + 17;
            } else {
                result = IsoUtilities.toUTCDateTimeFromDirectoryTime(data, pos[0]);
                pos[0] = pos[0] + 7;
            }
        }

        return result;
    }
}
