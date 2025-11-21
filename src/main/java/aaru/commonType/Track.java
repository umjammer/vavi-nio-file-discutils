//
// Aaru Data Preservation Suite
//
// License
//
//     This library is free software; you can redistribute it and/or modify
//     it under the terms of the GNU Lesser General Public License as
//     published by the Free Software Foundation; either version 2.1 of the
//     License, or (at your option) any later version.
//
//     This library is distributed in the hope that it will be useful, but
//     WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//     Lesser General Public License for more details.
//
//     You should have received a copy of the GNU Lesser General Public
//     License along with this library; if not, see <http://www.gnu.org/licenses/>.
//
//
// Copyright © 2011-2021 Natalia Portillo
//

package aaru.commonType;

import java.util.HashMap;
import java.util.Map;

import dotnet4j.io.Stream;


/** Track defining structure. */
public class Track {
    /** How many main channel / user data bytes are per sector in this track */
    public int bytesPerSector;
    /** Information that does not find space in this struct */
    public String description;
    /** Track ending sector */
    public long endSector;
    /** Which file stores this track */
    public String file;
    /** Starting at which byte is this track stored */
    public long fileOffset;
    /** What kind of file is storing this track */
    public String fileType;
    /** Which filter stores this track */
    public Stream filter;
    /** Indexes, 00 to 99 and sector offset */
    public final Map<Integer, Integer> indexes;
    /** Track pre-gap */
    public long preGap;
    /** How many main channel bytes per sector are in the file with this track */
    public int rawBytesPerSector;
    /** Track number, 1-started */
    public int sequence;
    /** Session this track belongs to */
    public short session;
    /** Track starting sector */
    public long startSector;
    /** Which file stores this track's sub-channel */
    public String subChannelFile;
    /** Which filter stores this track's sub-channel */
    public Stream subChannelFilter;
    /** Starting at which byte are this track's sub-channel stored */
    public long subChannelOffset;
    /** Type of sub-channel stored for this track */
    public TrackSubChannelType subChannelType;
    /** Partition type */
    public TrackType type;

    /** Initializes an empty instance of this structure */
    public Track() {
        indexes = new HashMap<>();
    }
}
