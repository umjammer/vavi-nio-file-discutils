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

package DiscUtils.Iso9660.RockRidge;

import DiscUtils.Iso9660.IsoUtilities;
import DiscUtils.Iso9660.Susp.SystemUseEntry;


public final class FileTimeSystemUseEntry extends SystemUseEntry {
    public enum Timestamps {
        None,
        Creation,
        Modify,
        __dummyEnum__0,
        Access,
        __dummyEnum__1,
        __dummyEnum__2,
        __dummyEnum__3,
        Attributes,
        __dummyEnum__4,
        __dummyEnum__5,
        __dummyEnum__6,
        __dummyEnum__7,
        __dummyEnum__8,
        __dummyEnum__9,
        __dummyEnum__10,
        Backup,
        __dummyEnum__11,
        __dummyEnum__12,
        __dummyEnum__13,
        __dummyEnum__14,
        __dummyEnum__15,
        __dummyEnum__16,
        __dummyEnum__17,
        __dummyEnum__18,
        __dummyEnum__19,
        __dummyEnum__20,
        __dummyEnum__21,
        __dummyEnum__22,
        __dummyEnum__23,
        __dummyEnum__24,
        __dummyEnum__25,
        Expiration,
        __dummyEnum__26,
        __dummyEnum__27,
        __dummyEnum__28,
        __dummyEnum__29,
        __dummyEnum__30,
        __dummyEnum__31,
        __dummyEnum__32,
        __dummyEnum__33,
        __dummyEnum__34,
        __dummyEnum__35,
        __dummyEnum__36,
        __dummyEnum__37,
        __dummyEnum__38,
        __dummyEnum__39,
        __dummyEnum__40,
        __dummyEnum__41,
        __dummyEnum__42,
        __dummyEnum__43,
        __dummyEnum__44,
        __dummyEnum__45,
        __dummyEnum__46,
        __dummyEnum__47,
        __dummyEnum__48,
        __dummyEnum__49,
        __dummyEnum__50,
        __dummyEnum__51,
        __dummyEnum__52,
        __dummyEnum__53,
        __dummyEnum__54,
        __dummyEnum__55,
        __dummyEnum__56,
        Effective;

        public static Timestamps valueOf(int value) {
            return values()[value];
        }
    }

    public long AccessTime;

    public long AttributesTime;

    public long BackupTime;

    public long CreationTime;

    public long EffectiveTime;

    public long ExpirationTime;

    public long ModifyTime;

    public Timestamps TimestampsPresent = Timestamps.None;

    public FileTimeSystemUseEntry(String name, byte length, byte version, byte[] data, int offset) {
        checkAndSetCommonProperties(name, length, version, (byte) 5, (byte) 1);
        byte flags = data[offset + 4];
        boolean longForm = (flags & 0x80) != 0;
        @SuppressWarnings("unused")
        int fieldLen = longForm ? 17 : 7;
        TimestampsPresent = Timestamps.valueOf(flags & 0x7F);
        int[] pos = new int[] {
            offset + 5
        };
        CreationTime = readTimestamp(Timestamps.Creation, data, longForm, pos);
        ModifyTime = readTimestamp(Timestamps.Modify, data, longForm, pos);
        AccessTime = readTimestamp(Timestamps.Access, data, longForm, pos);
        AttributesTime = readTimestamp(Timestamps.Attributes, data, longForm, pos);
        BackupTime = readTimestamp(Timestamps.Backup, data, longForm, pos);
        ExpirationTime = readTimestamp(Timestamps.Expiration, data, longForm, pos);
        EffectiveTime = readTimestamp(Timestamps.Effective, data, longForm, pos);
    }

    private long readTimestamp(Timestamps timestamp, byte[] data, boolean longForm, int[] pos) {
        long result = Long.MIN_VALUE;
        if ((TimestampsPresent.ordinal() & timestamp.ordinal()) != 0) {
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
