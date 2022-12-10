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


/** Type of subchannel in track */
public enum TrackSubChannelType {
    /** Track does not has subchannel dumped, or it's not a CD */
    None,
    /** Subchannel is packed and error corrected */
    Packed,
    /** Subchannel is interleaved */
    Raw,
    /** Subchannel is packed and comes interleaved with main channel in same file */
    PackedInterleaved,
    /** Subchannel is interleaved and comes interleaved with main channel in same file */
    RawInterleaved,
    /** Only Q subchannel is stored as 16 bytes */
    Q16,
    /** Only Q subchannel is stored as 16 bytes and comes interleaved with main channel in same file */
    Q16Interleaved
}
