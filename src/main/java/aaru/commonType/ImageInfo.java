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

import java.time.Instant;
import java.util.List;


/** Contains information about a dump image and its contents */
public class ImageInfo {
    /** Image contains partitions (or tracks for optical media) */
    public boolean hasPartitions;
    /** Image contains sessions (optical media only) */
    public boolean hasSessions;
    /** Size of the image without headers */
    public long imageSize;
    /** Sectors contained in the image */
    public long sectors;
    /** Size of sectors contained in the image */
    public int sectorSize;
    /** Media tags contained by the image */
    public List<MediaTagType> readableMediaTags;
    /** Sector tags contained by the image */
    public List<SectorTagType> readableSectorTags;
    /** Image version */
    public String version;
    /** Application that created the image */
    public String application;
    /** Version of the application that created the image */
    public String applicationVersion;
    /** Who (person) created the image? */
    public String creator;
    /** Image creation time */
    public Instant creationTime;
    /** Image last modification time */
    public Instant lastModificationTime;
    /** Title of the media represented by the image */
    public String mediaTitle;
    /** Image comments */
    public String comments;
    /** Manufacturer of the media represented by the image */
    public String mediaManufacturer;
    /** model of the media represented by the image */
    public String mediaModel;
    /** Serial number of the media represented by the image */
    public String mediaSerialNumber;
    /** Barcode of the media represented by the image */
    public String mediaBarcode;
    /** Part number of the media represented by the image */
    public String mediaPartNumber;
    /** Media type represented by the image */
    public /*MediaType*/ String mediaType;
    /** Number in sequence for the media represented by the image */
    public int mediaSequence;
    /** Last media of the sequence the media represented by the image corresponds to */
    public int lastMediaSequence;
    /** Manufacturer of the drive used to read the media represented by the image */
    public String driveManufacturer;
    /** model of the drive used to read the media represented by the image */
    public String driveModel;
    /** Serial number of the drive used to read the media represented by the image */
    public String driveSerialNumber;
    /** Firmware revision of the drive used to read the media represented by the image */
    public String driveFirmwareRevision;
    /** Type of the media represented by the image to use in XML sidecars */
    public aaru.commonType.XmlMediaType xmlMediaType;

    // CHS geometry...
    /** cylinders of the media represented by the image */
    public int cylinders;
    /** heads of the media represented by the image */
    public int heads;
    /** Sectors per track of the media represented by the image (for variable image, the smallest) */
    public int sectorsPerTrack;
}
