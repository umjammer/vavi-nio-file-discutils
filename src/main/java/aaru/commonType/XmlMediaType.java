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


/** Enumeration of media types defined in CICM metadata */
public enum XmlMediaType {
    /** Purely optical discs */
    OpticalDisc,
    /** Media that is physically block-based or abstracted like that */
    BlockMedia,
    /** Media that can be accessed by-byte or by-bit, like chips */
    LinearMedia,
    /** Media that can only store data when it is modulated to audio */
    AudioMedia
}
