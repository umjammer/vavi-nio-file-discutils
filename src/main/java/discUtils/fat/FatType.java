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

package discUtils.fat;

import java.util.Arrays;
import java.util.function.Consumer;

import vavi.util.ByteUtil;


/**
 * Enumeration of known FAT types.
 */
public enum FatType {
    /**
     * Represents no known FAT type.
     */
    None(0, "Unknown FAT") {
        @Override public boolean isEndOfChain(int val) {
            throw new IllegalStateException("Unknown FAT type");
        }
        @Override public int getNumEntries(byte[] buffer) {
            throw new IllegalStateException("Unknown FAT type");
        }
        @Override public boolean isBadCluster(int val) {
            throw new IllegalStateException("Unknown FAT type");
        }
        @Override public int getNext(int cluster, byte[] buffer) {
            throw new IllegalStateException("Unknown FAT type");
        }
        @Override public void setNext(int cluster, int next, byte[] buffer, Consumer<Integer> markDirty) {
            throw new IllegalStateException("Unknown FAT type");
        }
    },
    /**
     * Represents a 12-bit FAT.
     */
    Fat12(12, "Microsoft FAT12") {
        @Override public boolean isEndOfChain(int val) {
            return (val & 0x0fff) >= 0x0ff8;
        }
        @Override public int getNumEntries(byte[] buffer) {
            return buffer.length / 3 * 2;
        }
        @Override public boolean isBadCluster(int val) {
            return (val & 0x0fff) == 0x0ff7;
        }
        @Override public int getNext(int cluster, byte[] buffer) {
            if ((cluster & 1) != 0) {
                return (ByteUtil.readLeShort(buffer, cluster + cluster / 2) >>> 4) & 0x0fff;
            } else {
                return ByteUtil.readLeShort(buffer, cluster + cluster / 2) & 0x0fff;
            }
        }
        @Override public void setNext(int cluster, int next, byte[] buffer, Consumer<Integer> markDirty) {
            int offset = cluster + cluster / 2;
            markDirty.accept(offset);
            markDirty.accept(offset + 1);
            // On alternate sector boundaries, cluster info crosses two sectors
            short maskedOldVal;
            if ((cluster & 1) != 0) {
                next = next << 4;
                maskedOldVal = (short) (ByteUtil.readLeShort(buffer, offset) & 0x000f);
            } else {
                next = next & 0x0fff;
                maskedOldVal = (short) (ByteUtil.readLeShort(buffer, offset) & 0xf000);
            }
            short newVal = (short) (maskedOldVal | next);
            ByteUtil.writeLeShort(newVal, buffer, offset);
        }
    },
    /**
     * Represents a 16-bit FAT.
     */
    Fat16(16, "Microsoft FAT16") {
        @Override public boolean isEndOfChain(int val) {
            return (val & 0xffff) >= 0xfff8;
        }
        @Override public int getNumEntries(byte[] buffer) {
            return buffer.length / 2;
        }
        @Override public boolean isBadCluster(int val) {
            return (val & 0xffff) == 0xfff7;
        }
        @Override public int getNext(int cluster, byte[] buffer) {
            return ByteUtil.readLeShort(buffer, cluster * 2);
        }
        @Override public void setNext(int cluster, int next, byte[] buffer, Consumer<Integer> markDirty) {
            markDirty.accept(cluster * 2);
            ByteUtil.writeLeShort((short) next, buffer, cluster * 2);
        }
    },
    /**
     * Represents a 32-bit FAT.
     */
    Fat32(32, "Microsoft FAT32") {
        @Override public boolean isEndOfChain(int val) {
            return (val & 0x0fff_fff8) >= 0x0fff_fff8;
        }
        @Override public int getNumEntries(byte[] buffer) {
            return buffer.length / 4;
        }
        @Override public boolean isBadCluster(int val) {
            return (val & 0x0fff_ffff) == 0x0fff_fff7; // TODO bug report
        }
        @Override public int getNext(int cluster, byte[] buffer) {
            return ByteUtil.readLeInt(buffer, cluster * 4) & 0x0fff_ffff;
        }
        @Override public void setNext(int cluster, int next, byte[] buffer, Consumer<Integer> markDirty) {
            markDirty.accept(cluster * 4);
            int oldVal = ByteUtil.readLeInt(buffer, cluster * 4);
            int newVal = (oldVal & 0xf000_0000) | (next & 0x0fff_ffff);
            ByteUtil.writeLeInt(newVal, buffer, cluster * 4);
        }
    };

    private final int value;
    private final String friendlyName;

    public int getValue() {
        return value;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public abstract int getNumEntries(byte[] buffer);

    public abstract boolean isEndOfChain(int val);

    public abstract boolean isBadCluster(int val);

    public abstract int getNext(int cluster, byte[] buffer);

    public abstract void setNext(int cluster, int next, byte[] buffer, Consumer<Integer> markDirty);

    FatType(int value, String friendlyName) {
        this.value = value;
        this.friendlyName = friendlyName;
    }

    public static FatType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().get();
    }
}
