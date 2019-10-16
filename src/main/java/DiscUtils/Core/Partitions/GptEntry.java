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

package DiscUtils.Core.Partitions;

import java.nio.charset.Charset;
import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;


public class GptEntry implements Comparable<GptEntry> {
    public long Attributes;

    public long FirstUsedLogicalBlock;

    public UUID Identity;

    public long LastUsedLogicalBlock;

    public String Name;

    public UUID PartitionType;

    public GptEntry() {
        PartitionType = new UUID(0L, 0L);
        Identity = new UUID(0L, 0L);
        Name = "";
    }

    public String getFriendlyPartitionType() {
        String __dummyScrutVar0 = PartitionType.toString().toUpperCase();
        if (__dummyScrutVar0.equals("00000000-0000-0000-0000-000000000000")) {
            return "Unused";
        } else if (__dummyScrutVar0.equals("024DEE41-33E7-11D3-9D69-0008C781F39F")) {
            return "MBR Partition Scheme";
        } else if (__dummyScrutVar0.equals("C12A7328-F81F-11D2-BA4B-00A0C93EC93B")) {
            return "EFI System";
        } else if (__dummyScrutVar0.equals("21686148-6449-6E6F-744E-656564454649")) {
            return "BIOS Boot";
        } else if (__dummyScrutVar0.equals("E3C9E316-0B5C-4DB8-817D-F92DF00215AE")) {
            return "Microsoft Reserved";
        } else if (__dummyScrutVar0.equals("EBD0A0A2-B9E5-4433-87C0-68B6B72699C7")) {
            return "Windows Basic Data";
        } else if (__dummyScrutVar0.equals("5808C8AA-7E8F-42E0-85D2-E1E90434CFB3")) {
            return "Windows Logical Disk Manager Metadata";
        } else if (__dummyScrutVar0.equals("AF9B60A0-1431-4F62-BC68-3311714A69AD")) {
            return "Windows Logical Disk Manager Data";
        } else if (__dummyScrutVar0.equals("75894C1E-3AEB-11D3-B7C1-7B03A0000000")) {
            return "HP-UX Data";
        } else if (__dummyScrutVar0.equals("E2A1E728-32E3-11D6-A682-7B03A0000000")) {
            return "HP-UX Service";
        } else if (__dummyScrutVar0.equals("A19D880F-05FC-4D3B-A006-743F0F84911E")) {
            return "Linux RAID";
        } else if (__dummyScrutVar0.equals("0657FD6D-A4AB-43C4-84E5-0933C84B4F4F")) {
            return "Linux Swap";
        } else if (__dummyScrutVar0.equals("E6D6D379-F507-44C2-A23C-238F2A3DF928")) {
            return "Linux Logical Volume Manager";
        } else if (__dummyScrutVar0.equals("83BD6B9D-7F41-11DC-BE0B-001560B84F0F")) {
            return "FreeBSD Boot";
        } else if (__dummyScrutVar0.equals("516E7CB4-6ECF-11D6-8FF8-00022D09712B")) {
            return "FreeBSD Data";
        } else if (__dummyScrutVar0.equals("516E7CB5-6ECF-11D6-8FF8-00022D09712B")) {
            return "FreeBSD Swap";
        } else if (__dummyScrutVar0.equals("516E7CB6-6ECF-11D6-8FF8-00022D09712B")) {
            return "FreeBSD Unix File System";
        } else if (__dummyScrutVar0.equals("516E7CB8-6ECF-11D6-8FF8-00022D09712B")) {
            return "FreeBSD Vinum volume manager";
        } else if (__dummyScrutVar0.equals("516E7CBA-6ECF-11D6-8FF8-00022D09712B")) {
            return "FreeBSD ZFS";
        } else if (__dummyScrutVar0.equals("48465300-0000-11AA-AA11-00306543ECAC")) {
            return "Mac OS X HFS+";
        } else if (__dummyScrutVar0.equals("55465300-0000-11AA-AA11-00306543ECAC")) {
            return "Mac OS X UFS";
        } else if (__dummyScrutVar0.equals("6A898CC3-1DD2-11B2-99A6-080020736631")) {
            return "Mac OS X ZFS";
        } else if (__dummyScrutVar0.equals("52414944-0000-11AA-AA11-00306543ECAC")) {
            return "Mac OS X RAID";
        } else if (__dummyScrutVar0.equals("52414944-5F4F-11AA-AA11-00306543ECAC")) {
            return "Mac OS X RAID, Offline";
        } else if (__dummyScrutVar0.equals("426F6F74-0000-11AA-AA11-00306543ECAC")) {
            return "Mac OS X Boot";
        } else if (__dummyScrutVar0.equals("4C616265-6C00-11AA-AA11-00306543ECAC")) {
            return "Mac OS X Label";
        } else if (__dummyScrutVar0.equals("49F48D32-B10E-11DC-B99B-0019D1879648")) {
            return "NetBSD Swap";
        } else if (__dummyScrutVar0.equals("49F48D5A-B10E-11DC-B99B-0019D1879648")) {
            return "NetBSD Fast File System";
        } else if (__dummyScrutVar0.equals("49F48D82-B10E-11DC-B99B-0019D1879648")) {
            return "NetBSD Log-Structed File System";
        } else if (__dummyScrutVar0.equals("49F48DAA-B10E-11DC-B99B-0019D1879648")) {
            return "NetBSD RAID";
        } else if (__dummyScrutVar0.equals("2DB519C4-B10F-11DC-B99B-0019D1879648")) {
            return "NetBSD Concatenated";
        } else if (__dummyScrutVar0.equals("2DB519EC-B10F-11DC-B99B-0019D1879648")) {
            return "NetBSD Encrypted";
        } else {
            return "Unknown";
        }
    }

    public int compareTo(GptEntry other) {
        return (int) (FirstUsedLogicalBlock - other.FirstUsedLogicalBlock);
    }

    public void readFrom(byte[] buffer, int offset) {
        PartitionType = EndianUtilities.toGuidLittleEndian(buffer, offset + 0);
        Identity = EndianUtilities.toGuidLittleEndian(buffer, offset + 16);
        FirstUsedLogicalBlock = EndianUtilities.toInt64LittleEndian(buffer, offset + 32);
        LastUsedLogicalBlock = EndianUtilities.toInt64LittleEndian(buffer, offset + 40);
        Attributes = EndianUtilities.toUInt64LittleEndian(buffer, offset + 48);
        Name = new String(buffer, offset + 56, 72, Charset.forName("UTF-16LE")).replaceFirst("\0*$", "");
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(PartitionType, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(Identity, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(FirstUsedLogicalBlock, buffer, offset + 32);
        EndianUtilities.writeBytesLittleEndian(LastUsedLogicalBlock, buffer, offset + 40);
        EndianUtilities.writeBytesLittleEndian(Attributes, buffer, offset + 48);
        System.arraycopy((Name + new String(new char[36])).getBytes(Charset.forName("UTF-16LE")), 0, buffer, offset + 56, 36);
    }

}
