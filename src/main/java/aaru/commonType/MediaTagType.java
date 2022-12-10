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


/** Metadata present for each media. */
public enum MediaTagType {
    /** CD table of contents */
    CD_TOC,
    /** CD session information */
    CD_SessionInfo,
    /** CD full table of contents */
    CD_FullTOC,
    /** CD PMA */
    CD_PMA,
    /** CD Address-Time-In-Pregroove */
    CD_ATIP,
    /** CD-Text */
    CD_TEXT,
    /** CD Media Catalogue Number */
    CD_MCN,
    /** DVD/HD DVD Physical Format Information */
    DVD_PFI,
    /** DVD Lead-in Copyright Management Information */
    DVD_CMI,
    /** DVD disc key */
    DVD_DiscKey,
    /** DVD/HD DVD Burst Cutting Area */
    DVD_BCA,
    /** DVD/HD DVD Lead-in Disc Manufacturer Information */
    DVD_DMI,
    /** Media identifier */
    DVD_MediaIdentifier,
    /** Media key block */
    DVD_MKB,
    /** DVD-RAM/HD DVD-RAM DDS information */
    DVDRAM_DDS,
    /** DVD-RAM/HD DVD-RAM Medium status */
    DVDRAM_MediumStatus,
    /** DVD-RAM/HD DVD-RAM Spare area information */
    DVDRAM_SpareArea,
    /** DVD-R/-RW/HD DVD-R RMD in last border-out */
    DVDR_RMD,
    /** Pre-recorded information from DVD-R/-RW lead-in */
    DVDR_PreRecordedInfo,
    /** DVD-R/-RW/HD DVD-R media identifier */
    DVDR_MediaIdentifier,
    /** DVD-R/-RW/HD DVD-R physical format information */
    DVDR_PFI,
    /** ADIP information */
    DVD_ADIP,
    /** HD DVD Lead-in copyright protection information */
    HDDVD_CPI,
    /** HD DVD-R Medium Status */
    HDDVD_MediumStatus,
    /** DVD+/-R DL Layer capacity */
    DVDDL_LayerCapacity,
    /** DVD-R DL Middle Zone start address */
    DVDDL_MiddleZoneAddress,
    /** DVD-R DL Jump Interval Size */
    DVDDL_JumpIntervalSize,
    /** DVD-R DL Start LBA of the manual layer jump */
    DVDDL_ManualLayerJumpLBA,
    /** Blu-ray Disc Information */
    BD_DI,
    /** Blu-ray Burst Cutting Area */
    BD_BCA,
    /** Blu-ray Disc Definition Structure */
    BD_DDS,
    /** Blu-ray Cartridge Status */
    BD_CartridgeStatus,
    /** Blu-ray Status of Spare Area */
    BD_SpareArea,
    /** AACS volume identifier */
    AACS_VolumeIdentifier,
    /** AACS pre-recorded media serial number */
    AACS_SerialNumber,
    /** AACS media identifier */
    AACS_MediaIdentifier,
    /** Lead-in AACS media key block */
    AACS_MKB,
    /** AACS data keys */
    AACS_DataKeys,
    /** LBA extents flagged for bus encryption by AACS */
    AACS_LBAExtents,
    /** CPRM media key block in Lead-in */
    AACS_CPRM_MKB,
    /** Recognized layer formats in hybrid discs */
    Hybrid_RecognizedLayers,
    /** Disc write protection status */
    MMC_WriteProtection,
    /** Disc standard information */
    MMC_DiscInformation,
    /** Disc track resources information */
    MMC_TrackResourcesInformation,
    /** BD-R Pseudo-overwrite information */
    MMC_POWResourcesInformation,
    /** SCSI INQUIRY response */
    SCSI_INQUIRY,
    /** SCSI MODE PAGE 2Ah */
    SCSI_MODEPAGE_2A,
    /** ATA IDENTIFY DEVICE response */
    ATA_IDENTIFY,
    /** ATA IDENTIFY PACKET DEVICE response */
    ATAPI_IDENTIFY,
    /** PCMCIA/CardBus Card Information Structure */
    PCMCIA_CIS,
    /** SecureDigital CID */
    SD_CID,
    /** SecureDigital CSD */
    SD_CSD,
    /** SecureDigital SCR */
    SD_SCR,
    /** SecureDigital OCR */
    SD_OCR,
    /** MultiMediaCard CID */
    MMC_CID,
    /** MultiMediaCard CSD */
    MMC_CSD,
    /** MultiMediaCard OCR */
    MMC_OCR,
    /** MultiMediaCard Extended CSD */
    MMC_ExtendedCSD,
    /** Xbox Security Sector */
    Xbox_SecuritySector,
    /**
     * On floppy disks, data in last cylinder usually in a different format that contains duplication or
     * manufacturing information
     */
    Floppy_LeadOut,
    /** DVD Disc Control Blocks */
    DCB,
    /** Compact Disc First Track Pregap */
    CD_FirstTrackPregap,
    /** Compact Disc Lead-out */
    CD_LeadOut,
    /** SCSI MODE SENSE (6) */
    SCSI_MODESENSE_6,
    /** SCSI MODE SENSE (10) */
    SCSI_MODESENSE_10,
    /** USB descriptors */
    USB_Descriptors,
    /** XGD unlocked DMI */
    Xbox_DMI,
    /** XDG unlocked PFI */
    Xbox_PFI,
    /** Compact Disc Lead-in */
    CD_LeadIn,
    /** 8 bytes response that seems to define type of MiniDisc */
    MiniDiscType,
    /** 4 bytes response to vendor command D5h */
    MiniDiscD5,
    /** User TOC, contains fragments, track names, and can be from 1 to 3 sectors of 2336 bytes */
    MiniDiscUTOC,
    /** Not entirely clear kind of TOC that only appears on MD-DATA discs */
    MiniDiscDTOC,
    /** Decrypted DVD disc key */
    DVD_DiscKey_Decrypted
}
