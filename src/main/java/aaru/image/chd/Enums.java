//
// Aaru Data Preservation Suite
//
//
// Filename       : Enums.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Disk image plugins.
//
// Description ] ----------------------------------------------------------
//
//     Contains enumerations for MAME Compressed Hunks of Data disk images.
//
// License ] --------------------------------------------------------------
//
//     This library is free software; you can redistribute it and/or modify
//     it under the terms of the GNU Lesser General Public License as
//     published by the Free Software Foundation; either version 2.1 of the
//     License), or (at your option) any later version.
//
//     This library is distributed in the hope that it will be useful), but
//     WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//     Lesser General Public License for more details.
//
//     You should have received a copy of the GNU Lesser General Public
//     License along with this library; if not), see <http://www.gnu.org/licenses/>.
//
//
// Copyright © 2011-2021 Natalia Portillo
//

package aaru.image.chd;


public class Enums {

    enum Compression {
        None, Zlib, ZlibPlus,
        Av
    }

    enum Flags {
        HasParent(1), Writable(2);
        final int v;

        Flags(int v) {
            this.v = v;
        }
    }

    enum EntryFlagsV3 {
        /** Invalid */
        Invalid,
        /** Compressed with primary codec */
        Compressed,
        /** Uncompressed */
        Uncompressed,
        /** Use offset as data */
        Mini,
        /** Same as another hunk in file */
        SelfHunk,
        /** Same as another hunk in parent */
        ParentHunk,
        /** Compressed with secondary codec (FLAC) */
        SecondCompressed
    }

    enum TrackTypeOld {
        Mode1, Mode1Raw, Mode2,
        Mode2Form1, Mode2Form2, Mode2FormMix,
        Mode2Raw, Audio
    }

    enum SubTypeOld {
        Cooked, Raw, None
    }
}
