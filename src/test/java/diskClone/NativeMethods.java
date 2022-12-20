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
//
// Portions of this file from pinvoke.net
// (see http://www.pinvoke.net/termsofuse.htm)
//

package diskClone;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;


public interface NativeMethods extends Library {
    NativeMethods INSTANCE = Native.load("vssapi", NativeMethods.class);

    // [DllImport("vssapi.dll")]
    // internal static extern int CreateVssBackupComponentsInternal(out
    // IVssBackupComponents vssBackupCmpnts);
    int createVssBackupComponents(IVssBackupComponents[] vssBackupCmpnts);

    int createVssBackupComponents64(IVssBackupComponents[] vssBackupCmpnts);

    int vssFreeSnapshotProperties(int[][] pProperties);

    Pointer createFileW(String fileName,
                               FileAccess fileAccess,
                               FileShare fileShare,
                               int[][] securityAttributes,
                               FileMode creationDisposition,
                               int flags,
                               int[][] template);

    int deviceIoControl(Pointer hDevice,
                                   EIOControlCode dwIoControlCode,
                                   Object InBuffer,
                                   int nInBufferSize,
                                   Object OutBuffer,
                                   int nOutBufferSize,
                                   int[] pBytesReturned,
                                   int[][] lpOverlapped);

    int FORMAT_MESSAGE_ALLOCATE_BUFFER = 0x00000100;

    int FORMAT_MESSAGE_IGNORE_INSERTS = 0x00000200;

    int FORMAT_MESSAGE_FROM_SYSTEM = 0x00001000;

    int formatMessageW(int dwFlags,
                              int[][] lpSource,
                              int dwMessageId,
                              int dwLanguageId,
                              int[][][] lpBuffer,
                              int nSize,
                              int[][] pArguments);

    int[][] localFree(int[][] hMem);

    boolean getFileSizeEx(Pointer handle, long[] size);

    boolean setFilePointerEx(Pointer handle, long position, long[] pNewPointer, int MoveMethod);

    boolean readFile(Pointer handle, int[] buffer, int count, int[] numRead, int[] overlapped);

    class NtfsVolumeData extends Structure {

        public long volumeSerialNumber;

        public long numberSectors;

        public long totalClusters;

        public long freeClusters;

        public long totalReserved;

        public int bytesPerSector;

        public int bytesPerCluster;

        public int bytesPerFileRecordSegment;

        public int clustersPerFileRecordSegment;

        public long mftValidDataLength;

        public long mftStartLcn;

        public long mft2StartLcn;

        public long mftZoneStart;

        public long mftZoneEnd;

        protected List<String> getFieldOrder() {
            return Arrays.asList("volumeSerialNumber", "numberSectors", "totalClusters", "freeClusters",
                    "totalReserved", "bytesPerSector", "bytesPerCluster", "bytesPerFileRecordSegment",
                    "clustersPerFileRecordSegment", "mftValidDataLength", "mftStartLcn",
                    "mftZoneStart", "mftZoneEnd");
        }
    }

    class DiskExtent extends Structure {

        public int diskNumber;

        public long startingOffset;

        public long extentLength;

        protected List<String> getFieldOrder() {
            return Arrays.asList("diskNumber", "startingOffset", "extentLength");
        }
    }

    class DiskGeometry extends Structure {

        public long cylinders;

        public int mediaType;

        public int tracksPerCylinder;

        public int sectorsPerTrack;

        public int bytesPerSector;

        protected List<String> getFieldOrder() {
            return Arrays.asList("cylinders", "mediaType", "tracksPerCylinder", "sectorsPerTrack", "bytesPerSector");
        }
    }

    int ERROR_MORE_DATA = 234;

    enum EMethod {
        Buffered,
        InDirect,
        OutDirect,
        Neither
    }

    enum EFileDevice {
        __dummyEnum__0,
        Beep,
        CDRom,
        CDRomFileSytem,
        Controller,
        Datalink,
        Dfs,
        Disk,
        DiskFileSystem,
        FileSystem,
        InPortPort,
        Keyboard,
        Mailslot,
        MidiIn,
        MidiOut,
        Mouse,
        MultiUncProvider,
        NamedPipe,
        Network,
        NetworkBrowser,
        NetworkFileSystem,
        Null,
        ParellelPort,
        PhysicalNetcard,
        Printer,
        Scanner,
        SerialMousePort,
        SerialPort,
        Screen,
        Sound,
        Streams,
        Tape,
        TapeFileSystem,
        Transport,
        Unknown,
        Video,
        VirtualDisk,
        WaveIn,
        WaveOut,
        Port8042,
        NetworkRedirector,
        Battery,
        BusExtender,
        Modem,
        Vdm,
        MassStorage,
        Smb,
        Ks,
        Changer,
        Smartcard,
        Acpi,
        Dvd,
        FullscreenVideo,
        DfsFileSystem,
        DfsVolume,
        Serenum,
        Termsrv,
        Ksec,
        __dummyEnum__1,
        __dummyEnum__2,
        __dummyEnum__3,
        __dummyEnum__4,
        __dummyEnum__5,
        __dummyEnum__6,
        __dummyEnum__7,
        __dummyEnum__8,
        __dummyEnum__9,
        __dummyEnum__10,
        __dummyEnum__11,
        __dummyEnum__12,
        __dummyEnum__13,
        __dummyEnum__14,
        __dummyEnum__15,
        __dummyEnum__16,
        __dummyEnum__17,
        __dummyEnum__18,
        __dummyEnum__19,
        __dummyEnum__20,
        __dummyEnum__21,
        __dummyEnum__22,
        __dummyEnum__23,
        __dummyEnum__24,
        __dummyEnum__25,
        __dummyEnum__26,
        __dummyEnum__27,
        __dummyEnum__28,
        Volume
    }

    enum EIOControlCode {
        // STORAGE
        StorageBase,
        StorageCheckVerify,
        StorageCheckVerify2,
        // FileAccess.Any
        StorageMediaRemoval,
        StorageEjectMedia,
        StorageLoadMedia,
        StorageLoadMedia2,
        StorageReserve,
        StorageRelease,
        StorageFindNewDevices,
        StorageEjectionControl,
        StorageMcnControl,
        StorageGetMediaTypes,
        StorageGetMediaTypesEx,
        StorageResetBus,
        StorageResetDevice,
        StorageGetDeviceNumber,
        StoragePredictFailure,
        StorageObsoleteResetBus,
        StorageObsoleteResetDevice,
        // DISK
        DiskBase,
        DiskGetDriveGeometry,
        DiskGetPartitionInfo,
        DiskSetPartitionInfo,
        DiskGetDriveLayout,
        DiskSetDriveLayout,
        DiskVerify,
        DiskFormatTracks,
        DiskReassignBlocks,
        DiskPerformance,
        DiskIsWritable,
        DiskLogging,
        DiskFormatTracksEx,
        DiskHistogramStructure,
        DiskHistogramData,
        DiskHistogramReset,
        DiskRequestStructure,
        DiskRequestData,
        DiskControllerNumber,
        DiskGetLengthInfo,
        DiskSmartGetVersion,
        DiskSmartSendDriveCommand,
        DiskSmartRcvDriveData,
        DiskUpdateDriveSize,
        DiskGrowPartition,
        DiskGetCacheInformation,
        DiskSetCacheInformation,
        DiskDeleteDriveLayout,
        DiskFormatDrive,
        DiskSenseDevice,
        DiskCheckVerify,
        DiskMediaRemoval,
        DiskEjectMedia,
        DiskLoadMedia,
        DiskReserve,
        DiskRelease,
        DiskFindNewDevices,
        DiskGetMediaTypes,
        // CHANGER
        ChangerBase,
        ChangerGetParameters,
        ChangerGetStatus,
        ChangerGetProductData,
        ChangerSetAccess,
        ChangerGetElementStatus,
        ChangerInitializeElementStatus,
        ChangerSetPosition,
        ChangerExchangeMedium,
        ChangerMoveMedium,
        ChangerReinitializeTarget,
        ChangerQueryVolumeTags,
        // FILESYSTEM
        FsctlRequestOplockLevel1,
        FsctlRequestOplockLevel2,
        FsctlRequestBatchOplock,
        FsctlOplockBreakAcknowledge,
        FsctlOpBatchAckClosePending,
        FsctlOplockBreakNotify,
        FsctlLockVolume,
        FsctlUnlockVolume,
        FsctlDismountVolume,
        FsctlIsVolumeMounted,
        FsctlIsPathnameValid,
        FsctlMarkVolumeDirty,
        FsctlQueryRetrievalPointers,
        FsctlGetCompression,
        FsctlSetCompression,
        FsctlMarkAsSystemHive,
        FsctlOplockBreakAckNo2,
        FsctlInvalidateVolumes,
        FsctlQueryFatBpb,
        FsctlRequestFilterOplock,
        FsctlFileSystemGetStatistics,
        FsctlGetNtfsVolumeData,
        FsctlGetNtfsFileRecord,
        FsctlGetVolumeBitmap,
        FsctlGetRetrievalPointers,
        FsctlMoveFile,
        FsctlIsVolumeDirty,
        FsctlGetHfsInformation,
        FsctlAllowExtendedDasdIo,
        FsctlReadPropertyData,
        FsctlWritePropertyData,
        FsctlFindFilesBySid,
        FsctlDumpPropertyData,
        FsctlSetObjectId,
        FsctlGetObjectId,
        FsctlDeleteObjectId,
        FsctlSetReparsePoint,
        FsctlGetReparsePoint,
        FsctlDeleteReparsePoint,
        FsctlEnumUsnData,
        FsctlSecurityIdCheck,
        FsctlReadUsnJournal,
        FsctlSetObjectIdExtended,
        FsctlCreateOrGetObjectId,
        FsctlSetSparse,
        FsctlSetZeroData,
        FsctlQueryAllocatedRanges,
        FsctlEnableUpgrade,
        FsctlSetEncryption,
        FsctlEncryptionFsctlIo,
        FsctlWriteRawEncrypted,
        FsctlReadRawEncrypted,
        FsctlCreateUsnJournal,
        FsctlReadFileUsnData,
        FsctlWriteUsnCloseRecord,
        FsctlExtendVolume,
        FsctlQueryUsnJournal,
        FsctlDeleteUsnJournal,
        FsctlMarkHandle,
        FsctlSisCopyFile,
        FsctlSisLinkFiles,
        FsctlHsmMsg,
        FsctlNssControl,
        FsctlHsmData,
        FsctlRecallFile,
        FsctlNssRcontrol,
        // VIDEO
        VideoQuerySupportedBrightness,
        VideoQueryDisplayBrightness,
        VideoSetDisplayBrightness,
        // VOLUME
        VolumeGetDiskExtents
    }
}
