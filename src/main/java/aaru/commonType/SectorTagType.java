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


/** Metadata present for each sector (aka, "tag"). */
public enum SectorTagType {
    /** Apple's GCR sector tags, 12 bytes */
    AppleSectorTag,
    /** Sync frame from CD sector, 12 bytes */
    CdSectorSync,
    /** CD sector header, 4 bytes */
    CdSectorHeader,
    /** CD mode 2 sector subheader */
    CdSectorSubHeader,
    /** CD sector EDC, 4 bytes */
    CdSectorEdc,
    /** CD sector ECC P, 172 bytes */
    CdSectorEccP,
    /** CD sector ECC Q, 104 bytes */
    CdSectorEccQ,
    /** CD sector ECC (P and Q), 276 bytes */
    CdSectorEcc,
    /** CD sector subchannel, 96 bytes */
    CdSectorSubchannel,
    /** CD track ISRC, string, 12 bytes */
    CdTrackIsrc,
    /** CD track text, string, 13 bytes */
    CdTrackText,
    /** CD track flags, 1 byte */
    CdTrackFlags,
    /** DVD sector copyright information */
    DvdCmi,
    /** Floppy address mark (contents depend on underlying floppy format) */
    FloppyAddressMark,
    /** DVD sector title key, 5 bytes */
    DvdTitleKey,
    /** Decrypted DVD sector title key, 5 bytes */
    DvdTitleKeyDecrypted
}
