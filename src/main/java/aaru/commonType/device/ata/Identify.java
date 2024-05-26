//
// Aaru Data Preservation Suite
//
//
// Filename       : Identify.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Common structures for ATA devices.
//
// Description
//
//     Defines a high level interpretation of the ATA IDENTIFY response.
//
// License
//
//     Permission is hereby granted, free of charge, to any person obtaining a
//     copy of this software and associated documentation files (the
//     "Software"), to deal in the Software without restriction, including
//     without limitation the rights to use, copy, modify, merge, publish,
//     distribute, sublicense, and/or sell copies of the Software, and to
//     permit persons to whom the Software is furnished to do so, subject to
//     the following conditions:
//
//     The above copyright notice and this permission notice shall be included
//     in all copies or substantial portions of the Software.
//
//     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
//     OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//     MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//     IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//     CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//     TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//     SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
//
// Copyright © 2011-2021 Natalia Portillo
//

package aaru.commonType.device.ata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;

import vavi.util.ByteUtil;
import vavi.util.serdes.Element;
import vavi.util.serdes.Serdes;

import static java.lang.System.getLogger;


/** 
 * Information from following standards: T10-791D rev. 4c (ATA) T10-948D rev. 4c (ATA-2) T13-1153D rev. 18
 * (ATA/ATAPI-4) T13-1321D rev. 3 (ATA/ATAPI-5) T13-1410D rev. 3b (ATA/ATAPI-6) T13-1532D rev. 4b (ATA/ATAPI-7)
 * T13-1699D rev. 3f (ATA8-ACS) T13-1699D rev. 4a (ATA8-ACS) T13-2015D rev. 2 (ACS-2) T13-2161D rev. 5 (ACS-3) CF+
 * &amp; CF Specification rev. 1.4 (CFA)
 */
public class Identify {

    private static final Logger logger = getLogger(Identify.class.getName());

    /** capabilities flag bits. */
    public enum CapabilitiesBit {
        /** ATAPI: Interleaved DMA supported */
        InterleavedDMA(0x8000),
        /** ATAPI: Command queueing supported */
        CommandQueue(0x4000),
        /** Standby timer values are standard */
        StandardStandbyTimer(0x2000),
        /** ATAPI: Overlap operation supported */
        OverlapOperation(0x2000),
        /** ATAPI: ATA software reset required Obsoleted in ATA/ATAPI-4 */
        RequiresATASoftReset(0x1000),
        /** IORDY is supported */
        IORDY(0x0800),
        /** IORDY can be disabled */
        CanDisableIORDY(0x0400),
        /** LBA is supported */
        LBASupport(0x0200),
        /** DMA is supported */
        DMASupport(0x0100),
        /** Vendor unique Obsoleted in ATA/ATAPI-4 */
        VendorBit7(0x0080),
        /** Vendor unique Obsoleted in ATA/ATAPI-4 */
        VendorBit6(0x0040),
        /** Vendor unique Obsoleted in ATA/ATAPI-4 */
        VendorBit5(0x0020),
        /** Vendor unique Obsoleted in ATA/ATAPI-4 */
        VendorBit4(0x0010),
        /** Vendor unique Obsoleted in ATA/ATAPI-4 */
        VendorBit3(0x0008),
        /** Vendor unique Obsoleted in ATA/ATAPI-4 */
        VendorBit2(0x0004),
        /** Long Physical Alignment setting bit 1 */
        PhysicalAlignment1(0x0002),
        /** Long Physical Alignment setting bit 0 */
        PhysicalAlignment0(0x0001);
        final int v;

        CapabilitiesBit(int v) {
            this.v = v;
        }
    }

    /** More capabilities flag bits. */
    public enum CapabilitiesBit2 {
        /** MUST NOT be set */
        MustBeClear(0x8000),
        /** MUST be set */
        MustBeSet(0x4000),
//#pragma warning disable 1591
        Reserved13(0x2000), Reserved12(0x1000), Reserved11(0x0800),
        Reserved10(0x0400), Reserved09(0x0200), Reserved08(0x0100),
        Reserved07(0x0080), Reserved06(0x0040), Reserved05(0x0020),
        Reserved04(0x0010), Reserved03(0x0008), Reserved02(0x0004),
        Reserved01(0x0002),
//#pragma warning restore 1591
        /** Indicates a device specific minimum standby timer value */
        SpecificStandbyTimer(0x0001);
        final int v;

        CapabilitiesBit2(int v) {
            this.v = v;
        }
    }

    /** Even more capabilities flag bits. */
    public enum CapabilitiesBit3 {
        /** BLOCK ERASE EXT supported */
        BlockErase(0x0080),
        /** OVERWRITE EXT supported */
        Overwrite(0x0040),
        /** CRYPTO SCRAMBLE EXT supported */
        CryptoScramble(0x0020),
        /** Sanitize feature set is supported */
        Sanitize(0x0010),
        /** If unset), sanitize commands are specified by ACS-2 */
        SanitizeCommands(0x0008),
        /** SANITIZE ANTIFREEZE LOCK EXT is supported */
        SanitizeAntifreeze(0x0004),
        //#pragma warning disable 1591
        Reserved01(0x0002),
        //#pragma warning restore 1591
        /** Multiple logical sector setting is valid */
        MultipleValid(0x0001);
        final int v;

        CapabilitiesBit3(int v) {
            this.v = v;
        }
    }

    /** Command set flag bits. */
    public enum CommandSetBit {
        /** Already obsolete in ATA/ATAPI-4), reserved in ATA3 */
        Obsolete15(0x8000),
        /** NOP is supported */
        Nop(0x4000),
        /** READ BUFFER is supported */
        ReadBuffer(0x2000),
        /** WRITE BUFFER is supported */
        WriteBuffer(0x1000),
        /** Already obsolete in ATA/ATAPI-4), reserved in ATA3 */
        Obsolete11(0x0800),
        /** Host Protected Area is supported */
        HPA(0x0400),
        /** DEVICE RESET is supported */
        DeviceReset(0x0200),
        /** SERVICE interrupt is supported */
        Service(0x0100),
        /** Release is supported */
        Release(0x0080),
        /** Look-ahead is supported */
        LookAhead(0x0040),
        /** Write cache is supported */
        WriteCache(0x0020),
        /** PACKET command set is supported */
        Packet(0x0010),
        /** Power Management feature set is supported */
        PowerManagement(0x0008),
        /** Removable Media feature set is supported */
        RemovableMedia(0x0004),
        /** Security Mode feature set is supported */
        SecurityMode(0x0002),
        /** SMART feature set is supported */
        SMART(0x0001);
        final int v;

        CommandSetBit(int v) {
            this.v = v;
        }
    }

    /** More command set flag bits. */
    public enum CommandSetBit2 {
        /** MUST NOT be set */
        MustBeClear(0x8000),
        /** MUST BE SET */
        MustBeSet(0x4000),
        /** FLUSH CACHE EXT supported */
        FlushCacheExt(0x2000),
        /** FLUSH CACHE supported */
        FlushCache(0x1000),
        /** Device Configuration Overlay feature set supported */
        DCO(0x0800),
        /** 48-bit LBA supported */
        LBA48(0x0400),
        /** Automatic Acoustic Management supported */
        AAM(0x0200),
        /** SET MAX security extension supported */
        SetMax(0x0100),
        /** Address Offset Reserved Area Boot NCITS TR27:2001 */
        AddressOffsetReservedAreaBoot(0x0080),
        /** SET FEATURES required to spin-up */
        SetFeaturesRequired(0x0040),
        /** Power-Up in standby feature set supported */
        PowerUpInStandby(0x0020),
        /** Removable Media Status Notification feature set is supported */
        RemovableNotification(0x0010),
        /** Advanced Power Management feature set is supported */
        APM(0x0008),
        /** Compact Flash feature set is supported */
        CompactFlash(0x0004),
        /** READ DMA QUEUED and WRITE DMA QUEUED are supported */
        RWQueuedDMA(0x0002),
        /** DOWNLOAD MICROCODE is supported */
        DownloadMicrocode(0x0001);
        final int v;

        CommandSetBit2(int v) {
            this.v = v;
        }
    }

    /** Even more command set flag bits. */
    public enum CommandSetBit3 {
        /** MUST NOT be set */
        MustBeClear(0x8000),
        /** MUST BE SET */
        MustBeSet(0x4000),
        /** IDLE IMMEDIATE with UNLOAD FEATURE is supported */
        IdleImmediate(0x2000),
        /** Reserved for INCITS TR-37/2004 */
        Reserved12(0x1000),
        /** Reserved for INCITS TR-37/2004 */
        Reserved11(0x0800),
        /** URG bit is supported in WRITE STREAM DMA EXT and WRITE STREAM EXT */
        WriteURG(0x0400),
        /** URG bit is supported in READ STREAM DMA EXT and READ STREAM EXT */
        ReadURG(0x0200),
        /** 64-bit World Wide Name is supported */
        WWN(0x0100),
        /** WRITE DMA QUEUED FUA EXT is supported */
        FUAWriteQ(0x0080),
        /** WRITE DMA FUA EXT and WRITE MULTIPLE FUA EXT are supported */
        FUAWrite(0x0040),
        /** General Purpose Logging feature supported */
        GPL(0x0020),
        /** Streaming feature set is supported */
        Streaming(0x0010),
        /** Media Card Pass Through command set supported */
        MCPT(0x0008),
        /** Media serial number supported */
        MediaSerial(0x0004),
        /** SMART self-test supported */
        SMARTSelfTest(0x0002),
        /** SMART error logging supported */
        SMARTLog(0x0001);
        final int v;

        CommandSetBit3(int v) {
            this.v = v;
        }
    }

    /** Yet more command set flag bits. */
    public enum CommandSetBit4 {
        /** MUST NOT be set */
        MustBeClear(0x8000),
        /** MUST be set */
        MustBeSet(0x4000),
//#pragma warning disable 1591
        Reserved13(0x2000), Reserved12(0x1000), Reserved11(0x0800),
        Reserved10(0x0400),
//#pragma warning restore 1591
        /** DSN feature set is supported */
        DSN(0x0200),
        /** Accessible Max Address Configuration is supported */
        AMAC(0x0100),
        /** Extended Power Conditions is supported */
        ExtPowerCond(0x0080),
        /** Extended Status Reporting is supported */
        ExtStatusReport(0x0040),
        /** Free-fall Control feature set is supported */
        FreeFallControl(0x0020),
        /** Supports segmented feature in DOWNLOAD MICROCODE */
        SegmentedDownloadMicrocode(0x0010),
        /** READ/WRITE DMA EXT GPL are supported */
        RWDMAExtGpl(0x0008),
        /** WRITE UNCORRECTABLE is supported */
        WriteUnc(0x0004),
        /** Write/Read/Verify is supported */
        WRV(0x0002),
        /** Reserved for DT1825 */
        DT1825(0x0001);
        final int v;

        CommandSetBit4(int v) {
            this.v = v;
        }
    }

    /** Yet again more command set flag bits. */
    public enum CommandSetBit5 {
        /** Supports CFast Specification */
        CFast(0x8000),
        /** Deterministic read after TRIM is supported */
        DeterministicTrim(0x4000),
        /** Long physical sector alignment error reporting control is supported */
        LongPhysSectorAligError(0x2000),
        /** DEVICE CONFIGURATION IDENTIFY DMA and DEVICE CONFIGURATION SET DMA are supported */
        DeviceConfDMA(0x1000),
        /** READ BUFFER DMA is supported */
        ReadBufferDMA(0x0800),
        /** WRITE BUFFER DMA is supported */
        WriteBufferDMA(0x0400),
        /** SET PASSWORD DMA and SET UNLOCK DMA are supported */
        SetMaxDMA(0x0200),
        /** DOWNLOAD MICROCODE DMA is supported */
        DownloadMicroCodeDMA(0x0100),
        /** Reserved for IEEE-1667 */
        IEEE1667(0x0080),
        /** Optional ATA 28-bit commands are supported */
        Ata28(0x0040),
        /** Read zero after TRIM is supported */
        ReadZeroTrim(0x0020),
        /** Device encrypts all user data */
        Encrypted(0x0010),
        /** Extended number of user addressable sectors is supported */
        ExtSectors(0x0008),
        /** All write cache is non-volatile */
        AllCacheNV(0x0004),
        /** Zoned capabilities bit 1 */
        ZonedBit1(0x0002),
        /** Zoned capabilities bit 0 */
        ZonedBit0(0x0001);
        final int v;

        CommandSetBit5(int v) {
            this.v = v;
        }
    }

    /** Data set management flag bits. */
    public enum DataSetMgmtBit {
//#pragma warning disable 1591
        Reserved15(0x8000), Reserved14(0x4000), Reserved13(0x2000),
        Reserved12(0x1000), Reserved11(0x0800), Reserved10(0x0400),
        Reserved09(0x0200), Reserved08(0x0100), Reserved07(0x0080),
        Reserved06(0x0040), Reserved05(0x0020), Reserved04(0x0010),
        Reserved03(0x0008), Reserved02(0x0004), Reserved01(0x0002),
//#pragma warning restore 1591
        /** TRIM is supported */
        Trim(0x0001);
        final int v;

        DataSetMgmtBit(int v) {
            this.v = v;
        }
    }

    /** Device form factor */
    public enum DeviceFormFactorEnum {
        /** Size not reported */
        NotReported(0),
        /** 5.25" */
        FiveAndQuarter(1),
        /** 3.5" */
        ThreeAndHalf(2),
        /** 2.5" */
        TwoAndHalf(3),
        /** 1.8" */
        OnePointEight(4),
        /** Less than 1.8" */
        LessThanOnePointEight(5);
        final int v;

        DeviceFormFactorEnum(int v) {
            this.v = v;
        }
    }

    /** Extended identify flag bits. */
    //@flags
    public enum ExtendedIdentifyBit {
        /** Reserved */
        Reserved07(0x80),
        /** Reserved */
        Reserved06(0x40),
        /** Reserved */
        Reserved05(0x20),
        /** Reserved */
        Reserved04(0x10),
        /** Reserved */
        Reserved03(0x08),
        /** Identify word 88 is valid */
        Word88Valid(0x04),
        /** Identify words 64 to 70 are valid */
        Words64to70Valid(0x02),
        /** Identify words 54 to 58 are valid */
        Words54to58Valid(0x01);
        final int v;

        ExtendedIdentifyBit(int v) {
            this.v = v;
        }
    }

    /** General configuration flag bits. */
    //@flags
    public enum GeneralConfigurationBit {
        /** Set on ATAPI */
        NonMagnetic(0x8000),
        /** Format speed tolerance gap is required Obsoleted in ATA-2 */
        FormatGapReq(0x4000),
        /** Track offset option is available Obsoleted in ATA-2 */
        TrackOffset(0x2000),
        /** Data strobe offset option is available Obsoleted in ATA-2 */
        DataStrobeOffset(0x1000),
        /** Rotational speed tolerance is higher than 0),5% Obsoleted in ATA-2 */
        RotationalSpeedTolerance(0x0800),
        /** Disk transfer rate is &gt; 10 Mb/s Obsoleted in ATA-2 */
        UltraFastIDE(0x0400),
        /** Disk transfer rate is  &gt; 5 Mb/s but &lt;= 10 Mb/s Obsoleted in ATA-2 */
        FastIDE(0x0200),
        /** Disk transfer rate is &lt;= 5 Mb/s Obsoleted in ATA-2 */
        SlowIDE(0x0100),
        /** Drive uses removable media */
        Removable(0x0080),
        /** Drive is fixed Obsoleted in ATA/ATAPI-6 */
        Fixed(0x0040),
        /** Spindle motor control is implemented Obsoleted in ATA-2 */
        SpindleControl(0x0020),
        /** Head switch time is bigger than 15 µsec. Obsoleted in ATA-2 */
        HighHeadSwitch(0x0010),
        /** Drive is not MFM encoded Obsoleted in ATA-2 */
        NotMFM(0x0008),
        /** Drive is soft sectored Obsoleted in ATA-2 */
        SoftSector(0x0004),
        /** Response incomplete Since ATA/ATAPI-5 */
        IncompleteResponse(0x0004),
        /** Drive is hard sectored Obsoleted in ATA-2 */
        HardSector(0x0002),
        /** Reserved */
        Reserved(0x0001);
        final int v;

        GeneralConfigurationBit(int v) {
            this.v = v;
        }
    }

    /** Word 80 Major version */
    //@flags
    public enum MajorVersionBit {
//#pragma warning disable 1591
        Reserved15(0x8000), Reserved14(0x4000), Reserved13(0x2000),
        Reserved12(0x1000),
//#pragma warning restore 1591
        /** ACS-4 */
        ACS4(0x0800),
        /** ACS-3 */
        ACS3(0x0400),
        /** ACS-2 */
        ACS2(0x0200),
        /** ATA8-ACS */
        Ata8ACS(0x0100),
        /** ATA/ATAPI-7 */
        AtaAtapi7(0x0080),
        /** ATA/ATAPI-6 */
        AtaAtapi6(0x0040),
        /** ATA/ATAPI-5 */
        AtaAtapi5(0x0020),
        /** ATA/ATAPI-4 */
        AtaAtapi4(0x0010),
        /** ATA-3 */
        Ata3(0x0008),
        /** ATA-2 */
        Ata2(0x0004),
        /** ATA-1 */
        Ata1(0x0002),
//#pragma warning disable 1591
        Reserved00(0x0001);
//#pragma warning restore 1591
        final int v;

        MajorVersionBit(int v) {
            this.v = v;
        }
    }

    /** SATA capabilities flags */
    //@flags
    public enum SATACapabilitiesBit {
        /** Supports READ LOG DMA EXT */
        ReadLogDMAExt(0x8000),
        /** Supports device automatic partial to slumber transitions */
        DevSlumbTrans(0x4000),
        /** Supports host automatic partial to slumber transitions */
        HostSlumbTrans(0x2000),
        /** Supports NCQ priority */
        NCQPriority(0x1000),
        /** Supports unload while NCQ commands are outstanding */
        UnloadNCQ(0x0800),
        /** Supports PHY Event Counters */
        PHYEventCounter(0x0400),
        /** Supports receipt of host initiated power management requests */
        PowerReceipt(0x0200),
        /** Supports NCQ */
        NCQ(0x0100),
//#pragma warning disable 1591
        Reserved07(0x0080), Reserved06(0x0040), Reserved05(0x0020),
        Reserved04(0x0010),
//#pragma warning restore 1591
        /** Supports SATA Gen. 3 Signaling Speed (6.0Gb/s) */
        Gen3Speed(0x0008),
        /** Supports SATA Gen. 2 Signaling Speed (3.0Gb/s) */
        Gen2Speed(0x0004),
        /** Supports SATA Gen. 1 Signaling Speed (1.5Gb/s) */
        Gen1Speed(0x0002),
        /** MUST NOT be set */
        Clear(0x0001);
        final int v;

        SATACapabilitiesBit(int v) {
            this.v = v;
        }
    }

    /** More SATA capabilities flags */
    //@flags
    public enum SATACapabilitiesBit2 {
        //#pragma warning disable 1591
        Reserved15(0x8000), Reserved14(0x4000), Reserved13(0x2000),
        Reserved12(0x1000), Reserved11(0x0800), Reserved10(0x0400),
        Reserved09(0x0200), Reserved08(0x0100), Reserved07(0x0080),
        //#pragma warning restore 1591
        /** Supports RECEIVE FPDMA QUEUED and SEND FPDMA QUEUED */
        FPDMAQ(0x0040),
        /** Supports NCQ Queue Management */
        NCQMgmt(0x0020),
        /** ATAPI: Supports host environment detect */
        HostEnvDetect(0x0020),
        /** Supports NCQ streaming */
        NCQStream(0x0010),
        /** ATAPI: Supports device attention on slimline connected devices */
        DevAttSlimline(0x0010),
        /** Coded value indicating current negotiated Serial ATA signal speed */
        CurrentSpeedBit2(0x0008),
        /** Coded value indicating current negotiated Serial ATA signal speed */
        CurrentSpeedBit1(0x0004),
        /** Coded value indicating current negotiated Serial ATA signal speed */
        CurrentSpeedBit0(0x0002),
        /** MUST NOT be set */
        Clear(0x0001);
        final int v;

        SATACapabilitiesBit2(int v) {
            this.v = v;
        }
    }

    /** SATA features flags */
    //@flags
    public enum SATAFeaturesBit {
//#pragma warning disable 1591
        Reserved15(0x8000), Reserved14(0x4000), Reserved13(0x2000),
        Reserved12(0x1000), Reserved11(0x0800), Reserved10(0x0400),
        Reserved09(0x0200), Reserved08(0x0100),
//#pragma warning restore 1591
        /** Supports NCQ autosense */
        NCQAutoSense(0x0080),
        /** Automatic Partial to Slumber transitions are enabled */
        EnabledSlumber(0x0080),
        /** Supports Software Settings Preservation */
        SettingsPreserve(0x0040),
        /** Supports hardware feature control */
        HardwareFeatureControl(0x0020),
        /** ATAPI: Asynchronous notification */
        AsyncNotification(0x0020),
        /** Supports in-order data delivery */
        InOrderData(0x0010),
        /** Supports initiating power management */
        InitPowerMgmt(0x0008),
        /** Supports DMA Setup auto-activation */
        DMASetup(0x0004),
        /** Supports non-zero buffer offsets */
        NonZeroBufferOffset(0x0002),
        /** MUST NOT be set */
        Clear(0x0001);
        final int v;

        SATAFeaturesBit(int v) {
            this.v = v;
        }
    }

    /** SCT Command Transport flags */
    //@flags
    public enum SCTCommandTransportBit {
//#pragma warning disable 1591
        Vendor15(0x8000), Vendor14(0x4000), Vendor13(0x2000),
        Vendor12(0x1000), Reserved11(0x0800), Reserved10(0x0400),
        Reserved09(0x0200), Reserved08(0x0100), Reserved07(0x0080),
        Reserved06(0x0040),
//#pragma warning restore 1591
        /** SCT Command Transport Data Tables supported */
        DataTables(0x0020),
        /** SCT Command Transport Features Control supported */
        FeaturesControl(0x0010),
        /** SCT Command Transport Error Recovery Control supported */
        ErrorRecoveryControl(0x0008),
        /** SCT Command Transport Write Same supported */
        WriteSame(0x0004),
        /** SCT Command Transport Long Sector Address supported */
        LongSectorAccess(0x0002),
        /** SCT Command Transport supported */
        Supported(0x0001);
        final int v;

        SCTCommandTransportBit(int v) {
            this.v = v;
        }
    }

    /** Security status flag bits. */
    public enum SecurityStatusBit {
//#pragma warning disable 1591
        Reserved15(0x8000), Reserved14(0x4000), Reserved13(0x2000),
        Reserved12(0x1000), Reserved11(0x0800), Reserved10(0x0400),
        Reserved09(0x0200),
//#pragma warning restore 1591
        /** Maximum security level */
        Maximum(0x0100),
        //#pragma warning disable 1591
        Reserved07(0x0080), Reserved06(0x0040),
        //#pragma warning restore 1591
        /** Supports enhanced security erase */
        Enhanced(0x0020),
        /** Security count expired */
        Expired(0x0010),
        /** Security frozen */
        Frozen(0x0008),
        /** Security locked */
        Locked(0x0004),
        /** Security enabled */
        Enabled(0x0002),
        /** Security supported */
        Supported(0x0001);
        final int v;

        SecurityStatusBit(int v) {
            this.v = v;
        }
    }

    /** Specific configuration flags */
    public enum SpecificConfigurationEnum {
        /** Device requires SET FEATURES to spin up and IDENTIFY DEVICE response is incomplete */
        RequiresSetIncompleteResponse(0x37C8),
        /** Device requires SET FEATURES to spin up and IDENTIFY DEVICE response is complete */
        RequiresSetCompleteResponse(0x738C),
        /** Device does not requires SET FEATURES to spin up and IDENTIFY DEVICE response is incomplete */
        NotRequiresSetIncompleteResponse(0x8C73),
        /** Device does not requires SET FEATURES to spin up and IDENTIFY DEVICE response is complete */
        NotRequiresSetCompleteResponse(0xC837);
        final int v;

        SpecificConfigurationEnum(int v) {
            this.v = v;
        }
    }

    /** Transfer mode flags */
    public enum TransferMode {
//#pragma warning disable 1591
        Mode7(0x80), Mode6(0x40), Mode5(0x20),
        Mode4(0x10), Mode3(0x08), Mode2(0x04),
        Mode1(0x02), Mode0(0x01);
//#pragma warning restore 1591
        final int v;

        TransferMode(int v) {
            this.v = v;
        }
    }

    /** Trusted Computing flags */
    public enum TrustedComputingBit {
        /** MUST NOT be set */
        Clear(0x8000),
        /** MUST be set */
        Set(0x4000),
//#pragma warning disable 1591
        Reserved13(0x2000), Reserved12(0x1000), Reserved11(0x0800),
        Reserved10(0x0400), Reserved09(0x0200), Reserved08(0x0100),
        Reserved07(0x0080), Reserved06(0x0040), Reserved05(0x0020),
        Reserved04(0x0010), Reserved03(0x0008), Reserved02(0x0004),
        Reserved01(0x0002),
//#pragma warning restore 1591
        /** Trusted Computing feature set is supported */
        TrustedComputing(0x0001);
        final int v;

        TrustedComputingBit(int v) {
            this.v = v;
        }
    }

    /** IDENTIFY DEVICE decoded response */
    @Serdes
    public static class IdentifyDevice {
        /** 
         * Word 0 General device configuration On ATAPI devices: Bits 12 to 8 indicate device type as SCSI defined Bits 6
         * to 5: 0 = Device shall set DRQ within 3 ms of receiving PACKET 1 = Device shall assert INTRQ when DRQ is set to one
         * 2 = Device shall set DRQ within 50 µs of receiving PACKET Bits 1 to 0: 0 = 12 byte command packet 1 = 16 byte
         * command packet CompactFlash is 0x848A (non magnetic, removable, not MFM, hardsector, and UltraFAST)
         */
        @Element(sequence = 1, value = "unsigned short")
        public EnumSet<GeneralConfigurationBit> generalConfigurationBit;
        /** Word 1 cylinders in default translation mode Obsoleted in ATA/ATAPI-6 */
        @Element(sequence = 2)
        public short cylinders;
        /** Word 2 Specific configuration */
        @Element(sequence = 3, value = "unsigned short")
        public EnumSet<SpecificConfigurationEnum> specificConfigurationEnum;
        /** Word 3 heads in default translation mode Obsoleted in ATA/ATAPI-6 */
        @Element(sequence = 4)
        public short heads;
        /** Word 4 Unformatted bytes per track in default translation mode Obsoleted in ATA-2 */
        @Element(sequence = 5)
        public short unformattedBPT;
        /** Word 5 Unformatted bytes per sector in default translation mode Obsoleted in ATA-2 */
        @Element(sequence = 6)
        public short unformattedBPS;
        /** Word 6 Sectors per track in default translation mode Obsoleted in ATA/ATAPI-6 */
        @Element(sequence = 7)
        public short sectorsPerTrack;
        /** Words 7 to 8 CFA: Number of sectors per card */
        @Element(sequence = 8)
        public int sectorsPerCard;
        /** Word 9 Vendor unique Obsoleted in ATA/ATAPI-4 */
        @Element(sequence = 9)
        public short vendorWord9;
        /** Words 10 to 19 Device serial number, right justified, padded with spaces */
        @Element(sequence = 10, value = "20")
        public String serialNumber;
        /** 
         * Word 20 Manufacturer defined Obsoleted in ATA-2 0x0001 = single ported single sector buffer 0x0002 = dual
         * ported multi sector buffer 0x0003 = dual ported multi sector buffer with reading
         */
        @Element(sequence = 11)
        public short bufferType;
        /** Word 21 Size of buffer in 512 byte increments Obsoleted in ATA-2 */
        @Element(sequence = 12)
        public short bufferSize;
        /** Word 22 Bytes of ECC available in READ/WRITE LONG commands Obsoleted in ATA/ATAPI-4 */
        @Element(sequence = 13)
        public short eccBytes;
        /** Words 23 to 26 Firmware revision, left justified, padded with spaces */
        @Element(sequence = 14, value = "8")
        public String firmwareRevision;
        /** Words 27 to 46 model number, left justified, padded with spaces */
        @Element(sequence = 15, value = "40")
        public String model;
        /** 
         * Word 47 bits 7 to 0 Maximum number of sectors that can be transferred per interrupt on read and write multiple
         * commands
         */
        @Element(sequence = 16)
        public byte multipleMaxSectors;
        /** Word 47 bits 15 to 8 Vendor unique ATA/ATAPI-4 says it must be 0x80 */
        @Element(sequence = 17)
        public byte vendorWord47;
        /** 
         * Word 48 ATA-1: Set to 1 if it can perform doubleword I/O ATA-2 to ATA/ATAPI-7: Reserved ATA8-ACS: Trusted
         * Computing feature set
         */
        @Element(sequence = 18, value = "unsigned short")
        public EnumSet<TrustedComputingBit> trustedComputing;
        /** Word 49 capabilities */
        @Element(sequence = 19, value = "unsigned short")
        public EnumSet<CapabilitiesBit> capabilities;
        /** Word 50 capabilities */
        @Element(sequence = 20, value = "unsigned short")
        public EnumSet<CapabilitiesBit2> capabilities2;
        /** Word 51 bits 7 to 0 Vendor unique Obsoleted in ATA/ATAPI-4 */
        @Element(sequence = 21)
        public byte vendorWord51;
        /** Word 51 bits 15 to 8 Transfer timing mode in PIO Obsoleted in ATA/ATAPI-4 */
        @Element(sequence = 22)
        public byte pioTransferTimingMode;
        /** Word 52 bits 7 to 0 Vendor unique Obsoleted in ATA/ATAPI-4 */
        @Element(sequence = 23)
        public byte vendorWord52;
        /** Word 52 bits 15 to 8 Transfer timing mode in DMA Obsoleted in ATA/ATAPI-4 */
        @Element(sequence = 24)
        public byte dmaTransferTimingMode;
        /** Word 53 bits 7 to 0 Reports if words 54 to 58 are valid */
        @Element(sequence = 25, value = "unsigned byte")
        public EnumSet<ExtendedIdentifyBit> extendedIdentify;
        /** Word 53 bits 15 to 8 Free-fall Control Sensitivity */
        @Element(sequence = 26)
        public byte freeFallSensitivity;
        /** Word 54 cylinders in current translation mode Obsoleted in ATA/ATAPI-6 */
        @Element(sequence = 27)
        public short currentCylinders;
        /** Word 55 heads in current translation mode Obsoleted in ATA/ATAPI-6 */
        @Element(sequence = 28)
        public short currentHeads;
        /** Word 56 Sectors per track in current translation mode Obsoleted in ATA/ATAPI-6 */
        @Element(sequence = 29)
        public short currentSectorsPerTrack;
        /** Words 57 to 58 Total sectors currently user-addressable Obsoleted in ATA/ATAPI-6 */
        @Element(sequence = 30)
        public int currentSectors;
        /** Word 59 bits 7 to 0 Number of sectors currently set to transfer on a READ/WRITE MULTIPLE command */
        @Element(sequence = 31)
        public byte multipleSectorNumber;
        /** Word 59 bits 15 to 8 Indicates if <see cref="multipleSectorNumber" /> is valid */
        @Element(sequence = 32, value = "unsigned short")
        public EnumSet<CapabilitiesBit3> capabilities3;
        /** Words 60 to 61 If drive supports LBA, how many sectors are addressable using LBA */
        @Element(sequence = 33)
        public int lbaSectors;
        /** 
         * Word 62 bits 7 to 0 Single word DMA modes available Obsoleted in ATA/ATAPI-4 In ATAPI it's not obsolete,
         * indicates UDMA mode (UDMA7 is instead MDMA0)
         */
        @Element(sequence = 34, value = "unsigned byte")
        public EnumSet<TransferMode> dmaSupported;
        /** 
         * Word 62 bits 15 to 8 Single word DMA mode currently active Obsoleted in ATA/ATAPI-4 In ATAPI it's not
         * obsolete, bits 0 and 1 indicate MDMA mode+1, bit 10 indicates DMA is supported and bit 15 indicates DMADIR bit in
         * PACKET is required for DMA transfers
         */
        @Element(sequence = 35)
        public EnumSet<TransferMode> dmaActive;
        /** Word 63 bits 7 to 0 Multiword DMA modes available */
        @Element(sequence = 36)
        public EnumSet<TransferMode> mdmaSupported;
        /** Word 63 bits 15 to 8 Multiword DMA mode currently active */
        @Element(sequence = 37)
        public EnumSet<TransferMode> mdmaActive;

        /** Word 64 bits 7 to 0 Supported Advanced PIO transfer modes */
        @Element(sequence = 38, value = "unsigned byte")
        public EnumSet<TransferMode> apioSupported;
        /** Word 64 bits 15 to 8 Reserved */
        @Element(sequence = 39)
        public byte reservedWord64;
        /** Word 65 Minimum MDMA transfer cycle time per word in nanoseconds */
        @Element(sequence = 40)
        public short minMDMACycleTime;
        /** Word 66 Recommended MDMA transfer cycle time per word in nanoseconds */
        @Element(sequence = 41)
        public short recMDMACycleTime;
        /** Word 67 Minimum PIO transfer cycle time without flow control in nanoseconds */
        @Element(sequence = 42)
        public short minPIOCycleTimeNoFlow;
        /** Word 68 Minimum PIO transfer cycle time with IORDY flow control in nanoseconds */
        @Element(sequence = 43)
        public short minPIOCycleTimeFlow;

        /** Word 69 Additional supported */
        @Element(sequence = 44, value = "unsigned short")
        public EnumSet<CommandSetBit5> commandSet5;
        /** Word 70 Reserved */
        @Element(sequence = 45)
        public short reservedWord70;
        /** Word 71 ATAPI: Typical time in ns from receipt of PACKET to release bus */
        @Element(sequence = 46)
        public short packetBusRelease;
        /** Word 72 ATAPI: Typical time in ns from receipt of SERVICE to clear BSY */
        @Element(sequence = 47)
        public short serviceBusyClear;
        /** Word 73 Reserved */
        @Element(sequence = 48)
        public short reservedWord73;
        /** Word 74 Reserved */
        @Element(sequence = 49)
        public short reservedWord74;

        /** Word 75 Maximum Queue depth */
        @Element(sequence = 50)
        public short maxQueueDepth;

        /** Word 76 Serial ATA capabilities */
        @Element(sequence = 51, value = "unsigned short")
        public EnumSet<SATACapabilitiesBit> sataCapabilities;
        /** Word 77 Serial ATA Additional capabilities */
        @Element(sequence = 52, value = "unsigned short")
        public EnumSet<SATACapabilitiesBit2> sataCapabilities2;

        /** Word 78 Supported Serial ATA features */
        @Element(sequence = 53, value = "unsigned short")
        public EnumSet<SATAFeaturesBit> sataFeatures;
        /** Word 79 Enabled Serial ATA features */
        @Element(sequence = 54, value = "unsigned short")
        public EnumSet<SATAFeaturesBit> enabledSATAFeatures;

        /** Word 80 Major version of ATA/ATAPI standard supported */
        @Element(sequence = 55, value = "unsigned short")
        public EnumSet<MajorVersionBit> majorVersion;
        /** Word 81 Minimum version of ATA/ATAPI standard supported */
        @Element(sequence = 56)
        public short minorVersion;

        /** Word 82 Supported command/feature sets */
        @Element(sequence = 57, value = "unsigned short")
        public EnumSet<CommandSetBit> commandSet;
        /** Word 83 Supported command/feature sets */
        @Element(sequence = 58, value = "unsigned short")
        public EnumSet<CommandSetBit2> commandSet2;
        /** Word 84 Supported command/feature sets */
        @Element(sequence = 59, value = "unsigned short")
        public EnumSet<CommandSetBit3> commandSet3;

        /** Word 85 Enabled command/feature sets */
        @Element(sequence = 60, value = "unsigned short")
        public EnumSet<CommandSetBit> enabledCommandSet;
        /** Word 86 Enabled command/feature sets */
        @Element(sequence = 61, value = "unsigned short")
        public EnumSet<CommandSetBit2> enabledCommandSet2;
        /** Word 87 Enabled command/feature sets */
        @Element(sequence = 62, value = "unsigned short")
        public EnumSet<CommandSetBit3> enabledCommandSet3;

        /** Word 88 bits 7 to 0 Supported Ultra DMA transfer modes */
        @Element(sequence = 63, value = "unsigned byte")
        public EnumSet<TransferMode> udmaSupported;
        /** Word 88 bits 15 to 8 Selected Ultra DMA transfer modes */
        @Element(sequence = 64, value = "unsigned byte")
        public EnumSet<TransferMode> udmaActive;

        /** Word 89 Time required for security erase completion */
        @Element(sequence = 65)
        public short securityEraseTime;
        /** Word 90 Time required for enhanced security erase completion */
        @Element(sequence = 66)
        public short enhancedSecurityEraseTime;
        /** Word 91 Current advanced power management value */
        @Element(sequence = 67)
        public short currentAPM;

        /** Word 92 Master password revision code */
        @Element(sequence = 68)
        public short masterPasswordRevisionCode;
        /** Word 93 Hardware reset result */
        @Element(sequence = 69)
        public short hardwareResetResult;

        /** Word 94 bits 7 to 0 Current AAM value */
        @Element(sequence = 70)
        public byte currentAAM;
        /** Word 94 bits 15 to 8 Vendor's recommended AAM value */
        @Element(sequence = 71)
        public byte recommendedAAM;

        /** Word 95 Stream minimum request size */
        @Element(sequence = 72)
        public short streamMinReqSize;
        /** Word 96 Streaming transfer time in DMA */
        @Element(sequence = 73)
        public short streamTransferTimeDMA;
        /** Word 97 Streaming access latency in DMA and PIO */
        @Element(sequence = 74)
        public short streamAccessLatency;
        /** Words 98 to 99 Streaming performance granularity */
        @Element(sequence = 75)
        public int streamPerformanceGranularity;

        /** Words 100 to 103 48-bit LBA addressable sectors */
        @Element(sequence = 76)
        public long lba48Sectors;

        /** Word 104 Streaming transfer time in PIO */
        @Element(sequence = 77)
        public short streamTransferTimePIO;

        /** Word 105 Maximum number of 512-byte block per DATA SET MANAGEMENT command */
        @Element(sequence = 78)
        public short dataSetMgmtSize;

        /** 
         * Word 106 Bit 15 should be zero Bit 14 should be one Bit 13 set indicates device has multiple logical sectors
         * per physical sector Bit 12 set indicates logical sector has more than 256 words (512 bytes) Bits 11 to 4 are
         * reserved Bits 3 to 0 indicate power of two of logical sectors per physical sector
         */
        @Element(sequence = 79)
        public short physLogSectorSize;

        /** Word 107 Interseek delay for ISO-7779 acoustic testing, in microseconds */
        @Element(sequence = 80)
        public short interseekDelay;

        /** Words 108 to 111 World Wide Name */
        @Element(sequence = 81)
        public long wwn;

        /** Words 112 to 115 Reserved for wwn extension to 128 bit */
        @Element(sequence = 82)
        public long wwnExtension;

        /** Word 116 Reserved for technical report */
        @Element(sequence = 83)
        public short reservedWord116;

        /** Words 117 to 118 Words per logical sector */
        @Element(sequence = 84)
        public int logicalSectorWords;

        /** Word 119 Supported command/feature sets */
        @Element(sequence = 85, value = "unsigned short")
        public EnumSet<CommandSetBit4> commandSet4;
        /** Word 120 Supported command/feature sets */
        @Element(sequence = 86, value = "unsigned short")
        public EnumSet<CommandSetBit4> enabledCommandSet4;

        /** Words 121 to 125 Reserved */
        @Element(sequence = 87, value = "5")
        public short[] reservedWords121;

        /** Word 126 ATAPI byte count limit */
        @Element(sequence = 88)
        public short atapiByteCount;

        /** 
         * Word 127 Removable Media Status Notification feature set support Bits 15 to 2 are reserved Bits 1 to 0 must be
         * 0 for not supported or 1 for supported. 2 and 3 are reserved. Obsoleted in ATA8-ACS
         */
        @Element(sequence = 89)
        public short removableStatusSet;

        /** Word 128 Security status */
        @Element(sequence = 90)
        public SecurityStatusBit securityStatus;

        /** Words 129 to 159 */
        @Element(sequence = 91, value = "31")
        public short[] reservedWords129;

        /** 
         * Word 160 CFA power mode Bit 15 must be set Bit 13 indicates mode 1 is required for one or more commands Bit 12
         * indicates mode 1 is disabled Bits 11 to 0 indicates maximum current in mA
         */
        @Element(sequence = 92)
        public short cfaPowerMode;

        /** Words 161 to 167 Reserved for CFA */
        @Element(sequence = 93, value = "7")
        public short[] reservedCFA;

        /** Word 168 Bits 15 to 4, reserved Bits 3 to 0, device nominal form factor */
        @Element(sequence = 94)
        public DeviceFormFactorEnum deviceFormFactor;
        /** Word 169 DATA SET MANAGEMENT support */
        @Element(sequence = 95)
        public EnumSet<DataSetMgmtBit> dataSetMgmt;
        /** Words 170 to 173 Additional product identifier */
        @Element(sequence = 96, value = "8")
        public String additionalPID;

        /** Word 174 Reserved */
        @Element(sequence = 97)
        public short reservedWord174;
        /** Word 175 Reserved */
        @Element(sequence = 99)
        public short reservedWord175;

        /** Words 176 to 195 Current media serial number */
        @Element(sequence = 100, value = "40")
        public String mediaSerial;
        /** Words 196 to 205 Current media manufacturer */
        @Element(sequence = 101, value = "20")
        public String mediaManufacturer;

        /** Word 206 SCT Command Transport features */
        @Element(sequence = 102)
        public EnumSet<SCTCommandTransportBit> sctCommandTransport;

        /** Word 207 Reserved for CE-ATA */
        @Element(sequence = 103)
        public short reservedCEATAWord207;
        /** Word 208 Reserved for CE-ATA */
        @Element(sequence = 104)
        public short reservedCEATAWord208;

        /** 
         * Word 209 Alignment of logical block within a larger physical block Bit 15 shall be cleared to zero Bit 14
         * shall be set to one Bits 13 to 0 indicate logical sector offset within the first physical sector
         */
        @Element(sequence = 105)
        public short logicalAlignment;

        /** Words 210 to 211 Write/Read/Verify sector count mode 3 only */
        @Element(sequence = 106)
        public int wrvSectorCountMode3;
        /** Words 212 to 213 Write/Read/Verify sector count mode 2 only */
        @Element(sequence = 107)
        public int wrvSectorCountMode2;

        /** 
         * Word 214 NV Cache capabilities Bits 15 to 12 feature set version Bits 11 to 18 power mode feature set version
         * Bits 7 to 5 reserved Bit 4 feature set enabled Bits 3 to 2 reserved Bit 1 power mode feature set enabled Bit 0
         * power mode feature set supported
         */
        @Element(sequence = 108)
        public short nvCacheCaps;
        /** Words 215 to 216 NV Cache Size in Logical BLocks */
        @Element(sequence = 109)
        public int nvCacheSize;
        /** Word 217 Nominal media rotation rate In ACS-1 meant NV Cache read speed in MB/s */
        @Element(sequence = 110)
        public short nominalRotationRate;
        /** Word 218 NV Cache write speed in MB/s Reserved since ACS-2 */
        @Element(sequence = 111)
        public short nvCacheWriteSpeed;
        /** Word 219 bits 7 to 0 Estimated device spin up in seconds */
        @Element(sequence = 112)
        public byte nvEstimatedSpinUp;
        /** Word 219 bits 15 to 8 NV Cache reserved */
        @Element(sequence = 113)
        public byte nvReserved;

        /** Word 220 bits 7 to 0 Write/Read/Verify feature set current mode */
        @Element(sequence = 114)
        public byte wrvMode;
        /** Word 220 bits 15 to 8 Reserved */
        @Element(sequence = 115)
        public byte wrvReserved;

        /** Word 221 Reserved */
        @Element(sequence = 116)
        public short reservedWord221;

        /** 
         * Word 222 Transport major revision number Bits 15 to 12 indicate transport type. 0 parallel, 1 serial, 0xE
         * PCIe. Bits 11 to 0 indicate revision
         */
        @Element(sequence = 117)
        public short transportMajorVersion;
        /** Word 223 Transport minor revision number */
        @Element(sequence = 118)
        public short transportMinorVersion;

        /** Words 224 to 229 Reserved for CE-ATA */
        @Element(sequence = 119, value = "6")
        public short[] reservedCEATA224;

        /** Words 230 to 233 48-bit LBA if Word 69 bit 3 is set */
        @Element(sequence = 120)
        public long extendedUserSectors;

        /** Word 234 Minimum number of 512 byte units per DOWNLOAD MICROCODE mode 3 */
        @Element(sequence = 121)
        public short minDownloadMicroMode3;
        /** Word 235 Maximum number of 512 byte units per DOWNLOAD MICROCODE mode 3 */
        @Element(sequence = 122)
        public short maxDownloadMicroMode3;

        /** Words 236 to 254 */
        @Element(sequence = 123, value = "19")
        public short[] reservedWords;

        /** Word 255 bits 7 to 0 Should be 0xA5 */
        @Element(sequence = 124)
        public byte signature;
        /** Word 255 bits 15 to 8 Checksum */
        @Element(sequence = 125)
        public byte checksum;
    }

    /**
     * Decodes a raw IDENTIFY DEVICE response
     * @param IdentifyDeviceResponse Raw IDENTIFY DEVICE response
     * @return Decoded IDENTIFY DEVICE
     */
    public static Optional<IdentifyDevice> decode(byte[] IdentifyDeviceResponse) {
        if (IdentifyDeviceResponse == null)
            return Optional.empty();

        if (IdentifyDeviceResponse.length != 512) {
logger.log(Level.DEBUG, "ATA/ATAPI IDENTIFY decoder IDENTIFY response is different than 512 bytes, not decoding.");

            return Optional.empty();
        }

        IdentifyDevice ataId = new IdentifyDevice();
        try {
            Serdes.Util.deserialize(new ByteArrayInputStream(IdentifyDeviceResponse), ataId);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        ataId.wwn = descrambleWWN(ataId.wwn);
        ataId.wwnExtension = descrambleWWN(ataId.wwnExtension);

        ataId.serialNumber = descrambleATAString(IdentifyDeviceResponse, 10 * 2, 20);
        ataId.firmwareRevision = descrambleATAString(IdentifyDeviceResponse, 23 * 2, 8);
        ataId.model = descrambleATAString(IdentifyDeviceResponse, 27 * 2, 40);
        ataId.additionalPID = descrambleATAString(IdentifyDeviceResponse, 170 * 2, 8);
        ataId.mediaSerial = descrambleATAString(IdentifyDeviceResponse, 176 * 2, 40);
        ataId.mediaManufacturer = descrambleATAString(IdentifyDeviceResponse, 196 * 2, 20);

        return Optional.of(ataId);
    }

    /**
     * Encodes a raw IDENTIFY DEVICE response
     * @param identify Decoded IDENTIFY DEVICE
     * @return Raw IDENTIFY DEVICE response
     */
    public static byte[] encode(IdentifyDevice identify) {
        if (identify == null)
            return null;

        IdentifyDevice ataId = identify;

        ataId.wwn = descrambleWWN(ataId.wwn);
        ataId.wwnExtension = descrambleWWN(ataId.wwnExtension);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Serdes.Util.serialize(ataId, baos);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        byte[] buf = baos.toByteArray();
        byte[] str = scrambleATAString(ataId.serialNumber, 20);
        System.arraycopy(str, 0, buf, 10 * 2, 20);
        str = scrambleATAString(ataId.firmwareRevision, 8);
        System.arraycopy(str, 0, buf, 23 * 2, 8);
        str = scrambleATAString(ataId.model, 40);
        System.arraycopy(str, 0, buf, 27 * 2, 40);
        str = scrambleATAString(ataId.additionalPID, 8);
        System.arraycopy(str, 0, buf, 170 * 2, 8);
        str = scrambleATAString(ataId.mediaSerial, 40);
        System.arraycopy(str, 0, buf, 176 * 2, 40);
        str = scrambleATAString(ataId.mediaManufacturer, 20);
        System.arraycopy(str, 0, buf, 196 * 2, 20);

        return buf;
    }

    static long descrambleWWN(long wwn) {
        byte[] qwb = ByteUtil.getLeBytes(wwn);
        byte[] qWord = new byte[8];

        qWord[7] = qwb[1];
        qWord[6] = qwb[0];
        qWord[5] = qwb[3];
        qWord[4] = qwb[2];
        qWord[3] = qwb[5];
        qWord[2] = qwb[4];
        qWord[1] = qwb[7];
        qWord[0] = qwb[6];

        return ByteUtil.readLeLong(qWord, 0);
    }

    static String descrambleATAString(byte[] buffer, int offset, int length) {
        byte[] outBuf = buffer[offset + length - 1] != 0x00 ? new byte[length + 1] : new byte[length];

        for (int i = 0; i < length; i += 2) {
            outBuf[i] = buffer[offset + i + 1];
            outBuf[i + 1] = buffer[offset + i];
        }

        String outStr = new String(outBuf).replaceFirst("\u0000*$", "");

        return outStr.trim();
    }

    static byte[] scrambleATAString(String str, int length) {
        byte[] buf = new byte[length];

        for (int i = 0; i < length; i++)
            buf[i] = 0x20;

        if (str == null)
            return buf;

        byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);

        if (bytes.length % 2 != 0) {
            byte[] tmp = new byte[bytes.length + 1];
            tmp[tmp.length - 1] = 0x20;
            System.arraycopy(bytes, 0, tmp, 0, bytes.length);
            bytes = tmp;
        }

        for (int i = 0; i < bytes.length; i += 2) {
            buf[i] = bytes[i + 1];
            buf[i + 1] = bytes[i];
        }

        return buf;
    }
}
