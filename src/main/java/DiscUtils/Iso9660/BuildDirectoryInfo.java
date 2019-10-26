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

package DiscUtils.Iso9660;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.Util.MathUtilities;


/**
 * Represents a directory that will be built into the ISO image.
 */
public final class BuildDirectoryInfo extends BuildDirectoryMember {
    public static final Comparator<BuildDirectoryInfo> PathTableSortComparison = new PathTableComparison();

    private final Map<String, BuildDirectoryMember> _members;

    private final BuildDirectoryInfo _parent;

    private List<BuildDirectoryMember> _sortedMembers;

    public BuildDirectoryInfo(String name, BuildDirectoryInfo parent) {
        super(name, makeShortDirName(name, parent));
        _parent = parent == null ? this : parent;
        __HierarchyDepth = parent == null ? 0 : parent.getHierarchyDepth() + 1;
        _members = new HashMap<>();
    }

    private int __HierarchyDepth;

    public int getHierarchyDepth() {
        return __HierarchyDepth;
    }

    /**
     * The parent directory, or {@code null} if none.
     */
    public BuildDirectoryInfo getParent() {
        return _parent;
    }

    /**
     * Gets the specified child directory or file.
     *
     * @param name The name of the file or directory to get.
     * @param member The member found (or {@code null}).
     * @return {@code true} if the specified member was found.
     */
    public boolean tryGetMember(String name, BuildDirectoryMember[] member) {
        boolean result = _members.containsKey(name);
        member[0] = _members.get(name);
        return result;
    }

    public void add(BuildDirectoryMember member) {
        _members.put(member.getName(), member);
        _sortedMembers = null;
    }

    public long getDataSize(Charset enc) {
        List<BuildDirectoryMember> sorted = getSortedMembers();
        long total = 34 * 2;
        for (BuildDirectoryMember m : sorted) {
            // Two pseudo entries (self & parent)
            int recordSize = m.getDirectoryRecordSize(enc);
            // If this record would span a sector boundary, then the current sector is
            // zero-padded, and the record goes at the start of the next sector.
            if (total % IsoUtilities.SectorSize + recordSize > IsoUtilities.SectorSize) {
                long padLength = IsoUtilities.SectorSize - total % IsoUtilities.SectorSize;
                total += padLength;
            }

            total += recordSize;
        }
        return MathUtilities.roundUp(total, IsoUtilities.SectorSize);
    }

    public int getPathTableEntrySize(Charset enc) {
        int nameBytes = pickName(null, enc).getBytes(enc).length;
        return 8 + nameBytes + ((nameBytes & 0x1) == 1 ? 1 : 0);
    }

    public int write(byte[] buffer, int offset, Map<BuildDirectoryMember, Integer> locationTable, Charset enc) {
        int pos = 0;
        List<BuildDirectoryMember> sorted = getSortedMembers();
        // Two pseudo entries, effectively '.' and '..'
        pos += writeMember(this, "\0", Charset.forName("ASCII"), buffer, offset + pos, locationTable, enc);
        pos += writeMember(_parent, "\u0001", Charset.forName("ASCII"), buffer, offset + pos, locationTable, enc);
        for (BuildDirectoryMember m : sorted) {
            int recordSize = m.getDirectoryRecordSize(enc);
            if (pos % IsoUtilities.SectorSize + recordSize > IsoUtilities.SectorSize) {
                int padLength = IsoUtilities.SectorSize - pos % IsoUtilities.SectorSize;
                Arrays.fill(buffer, offset + pos, offset + pos + padLength, (byte) 0);
                pos += padLength;
            }

            pos += writeMember(m, null, enc, buffer, offset + pos, locationTable, enc);
        }
        // Ensure final padding data is zero'd
        int finalPadLength = MathUtilities.roundUp(pos, IsoUtilities.SectorSize) - pos;
        Arrays.fill(buffer, offset + pos, offset + pos + finalPadLength, (byte) 0);
        return pos + finalPadLength;
    }

    private static int writeMember(BuildDirectoryMember m,
                                   String nameOverride,
                                   Charset nameEnc,
                                   byte[] buffer,
                                   int offset,
                                   Map<BuildDirectoryMember, Integer> locationTable,
                                   Charset dataEnc) {
        DirectoryRecord dr = new DirectoryRecord();
        dr.FileIdentifier = m.pickName(nameOverride, nameEnc);
        dr.LocationOfExtent = locationTable.get(m);
        dr.DataLength = (int) m.getDataSize(dataEnc);
        dr.RecordingDateAndTime = m.getCreationTime();
        dr.Flags = EnumSet.of(m instanceof BuildDirectoryInfo ? FileFlags.Directory : FileFlags.None);
        return dr.writeTo(buffer, offset, nameEnc);
    }

    private static String makeShortDirName(String longName, BuildDirectoryInfo dir) {
        if (IsoUtilities.isValidDirectoryName(longName)) {
            return longName;
        }

        char[] shortNameChars = longName.toUpperCase().toCharArray();
        for (int i = 0; i < shortNameChars.length; ++i) {
            if (!IsoUtilities.isValidDChar(shortNameChars[i]) && shortNameChars[i] != '.' && shortNameChars[i] != ';') {
                shortNameChars[i] = '_';
            }
        }
        return new String(shortNameChars);
    }

    private List<BuildDirectoryMember> getSortedMembers() {
        if (_sortedMembers == null) {
            List<BuildDirectoryMember> sorted = new ArrayList<>(_members.values());
            Collections.sort(sorted, SortedComparison);
            _sortedMembers = sorted;
        }

        return _sortedMembers;
    }

    private static class PathTableComparison implements Comparator<BuildDirectoryInfo> {
        public int compare(BuildDirectoryInfo x, BuildDirectoryInfo y) {
            if (x.getHierarchyDepth() != y.getHierarchyDepth()) {
                return x.getHierarchyDepth() - y.getHierarchyDepth();
            }

            if (x.getParent() != y.getParent()) {
                return compare(x.getParent(), y.getParent());
            }

            return compareNames(x.getName(), y.getName(), ' ');
        }

        private static int compareNames(String x, String y, char padChar) {
            int max = Math.max(x.length(), y.length());
            for (int i = 0; i < max; ++i) {
                char xChar = i < x.length() ? x.charAt(i) : padChar;
                char yChar = i < y.length() ? y.charAt(i) : padChar;
                if (xChar != yChar) {
                    return xChar - yChar;
                }
            }
            return 0;
        }
    }
}
