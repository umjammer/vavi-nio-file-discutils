//
// Aaru Data Preservation Suite
//
//
// Filename       : Constants.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Disk image plugins.
//
// Description ] ----------------------------------------------------------
//
//     Contains static finalants for MAME Compressed Hunks of Data disk images.
//
// License ] --------------------------------------------------------------
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

package aaru.image.chd;


public class Constants {

    /** "GDDD" */
    static final int HARD_DISK_METADATA = 0x47444444;
    /** "IDNT" */
    static final int HARD_DISK_IDENT_METADATA = 0x49444E54;
    /** "KEY " */
    static final int HARD_DISK_KEY_METADATA = 0x4B455920;
    /** "CIS " */
    static final int PCMCIA_CIS_METADATA = 0x43495320;
    /** "CHCD" */
    static final int CDROM_OLD_METADATA = 0x43484344;
    /** "CHTR" */
    static final int CDROM_TRACK_METADATA = 0x43485452;
    /** "CHT2" */
    static final int CDROM_TRACK_METADATA2 = 0x43485432;
    /** "CHGT" */
    static final int GDROM_OLD_METADATA = 0x43484754;
    /** "CHGD" */
    static final int GDROM_METADATA = 0x43484744;
    /** "AVAV" */
    static final int AV_METADATA = 0x41564156;
    /** "AVLD" */
    static final int AV_LASER_DISC_METADATA = 0x41564C44;

    static final String REGEX_METADATA_HDD =
            "CYLS:(\\d+),HEADS:(\\d+),SECS:(\\d+),BPS:(\\d+)";
    static final String REGEX_METADATA_CDROM =
            "TRACK:(\\d+) TYPE:(\\S+) SUBTYPE:(\\S+) FRAMES:(\\d+)";
    static final String REGEX_METADATA_CDROM2 =
            "TRACK:(\\d+) TYPE:(\\S+) SUBTYPE:(\\S+) FRAMES:(\\d+) PREGAP:(\\d+) PGTYPE:(\\S+) PGSUB:(\\S+) POSTGAP:(\\d+)";
    static final String REGEX_METADATA_GDROM =
            "TRACK:(\\d+) TYPE:(\\S+) SUBTYPE:(\\S+) FRAMES:(\\d+) PAD:(\\d+) PREGAP:(\\d+) PGTYPE:(\\S+) PGSUB:(\\S+) POSTGAP:(\\d+)";

    static final String TRACK_TYPE_MODE1 = "MODE1";
    static final String TRACK_TYPE_MODE1_2K = "MODE1/2048";
    static final String TRACK_TYPE_MODE1_RAW = "MODE1_RAW";
    static final String TRACK_TYPE_MODE1_RAW_2K = "MODE1/2352";
    static final String TRACK_TYPE_MODE2 = "MODE2";
    static final String TRACK_TYPE_MODE2_2K = "MODE2/2336";
    static final String TRACK_TYPE_MODE2_F1 = "MODE2_FORM1";
    static final String TRACK_TYPE_MODE2_F1_2K = "MODE2/2048";
    static final String TRACK_TYPE_MODE2_F2 = "MODE2_FORM2";
    static final String TRACK_TYPE_MODE2_F2_2K = "MODE2/2324";
    static final String TRACK_TYPE_MODE2_FM = "MODE2_FORM_MIX";
    static final String TRACK_TYPE_MODE2_RAW = "MODE2_RAW";
    static final String TRACK_TYPE_MODE2_RAW_2K = "MODE2/2352";
    static final String TRACK_TYPE_AUDIO = "AUDIO";

    static final String SUB_TYPE_COOKED = "RW";
    static final String SUB_TYPE_RAW = "RW_RAW";
    static final String SUB_TYPE_NONE = "NONE";

    static final int MAX_CACHE_SIZE = 16777216;
}
