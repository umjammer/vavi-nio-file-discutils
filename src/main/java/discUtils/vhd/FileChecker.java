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

package discUtils.vhd;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.UUID;

import discUtils.core.Geometry;
import discUtils.core.InvalidFileSystemException;
import discUtils.core.ReportLevels;
import discUtils.core.internal.Utilities;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * VHD file format verifier, that identifies corrupt VHD files.
 */
public class FileChecker {
    private static final UUID EMPTY = new UUID(0L, 0L);

    private final Stream fileStream;

    private Footer footer;

    private DynamicHeader dynamicHeader;

    private PrintWriter report;

    private EnumSet<ReportLevels> reportLevels;

    private EnumSet<ReportLevels> levelsDetected;

    private static final ReportLevels levelsConsideredFail = ReportLevels.Errors;

    /**
     * Initializes a new instance of the FileChecker class.
     *
     * @param stream The VHD file stream.
     */
    public FileChecker(Stream stream) {
        fileStream = stream;
    }

    /**
     * Verifies the VHD file, generating a report and a pass/fail indication.
     *
     * @param reportOutput The destination for the report.
     * @param levels How verbose the report should be.
     * @return {@code true} if the file is valid, else false.
     */
    public boolean check(PrintWriter reportOutput, EnumSet<ReportLevels> levels) {
        report = reportOutput;
        reportLevels = levels;
        levelsDetected = EnumSet.noneOf(ReportLevels.class);
        try {
            doCheck();
        } catch (AbortException ae) {
            reportError("File system check aborted: " + ae);
            return false;
        } catch (Exception e) {
            reportError("File system check aborted with exception: " + e);
            return false;
        }

        return !levelsDetected.contains(levelsConsideredFail);
    }

    private static void abort() throws IOException {
        throw new AbortException();
    }

    private void doCheck() throws IOException {
        checkFooter();
        if (footer == null || footer.diskType != FileType.Fixed) {
            checkHeader();
        }

        if (footer == null) {
            reportError("Unable to continue - no valid header or footer");
            abort();
        }

        checkFooterFields();
        if (footer.diskType != FileType.Fixed) {
            checkDynamicHeader();
            checkBat();
        }
    }

    private void checkBat() {
        int batSize = MathUtilities.roundUp(dynamicHeader.maxTableEntries * 4, Sizes.Sector);
        if (dynamicHeader.tableOffset > fileStream.getLength() - batSize) {
            reportError("BAT: BAT extends beyond end of file");
            return;
        }

        fileStream.position(dynamicHeader.tableOffset);
        byte[] batData = StreamUtilities.readExact(fileStream, batSize);
        int[] bat = new int[batSize / 4];
        for (int i = 0; i < bat.length; ++i) {
            bat[i] = EndianUtilities.toUInt32BigEndian(batData, i * 4);
        }
        for (int i = dynamicHeader.maxTableEntries; i < bat.length; ++i) {
            if (bat[i] != 0xffffffff) {
                reportError("BAT: Padding record '" + i + "' should be 0xFFFFFFFF");
            }
        }
        int dataStartSector = 0xffffffff;
        for (int i = 0; i < dynamicHeader.maxTableEntries; ++i) {
            if (bat[i] < dataStartSector) {
                dataStartSector = bat[i];
            }
        }
        if (dataStartSector == 0xffffffff) {
            return;
        }

        long dataStart = (long) dataStartSector * Sizes.Sector;
        int blockBitmapSize = MathUtilities.roundUp(dynamicHeader.blockSize / Sizes.Sector / 8, Sizes.Sector);
        int storedBlockSize = dynamicHeader.blockSize + blockBitmapSize;
        boolean[] seenBlocks = new boolean[dynamicHeader.maxTableEntries];
        for (int i = 0; i < dynamicHeader.maxTableEntries; ++i) {
            if (bat[i] != 0xffffffff) {
                long absPos = (long) bat[i] * Sizes.Sector;
                if (absPos + storedBlockSize > fileStream.getLength()) {
                    reportError("BAT: block stored beyond end of stream");
                }

                if ((absPos - dataStart) % storedBlockSize != 0) {
                    reportError("BAT: block stored at invalid start sector (not a multiple of size of a stored block)");
                }

                int streamBlockIdx = (int) ((absPos - dataStart) / storedBlockSize);
                if (seenBlocks[streamBlockIdx]) {
                    reportError("BAT: multiple blocks occupying same file space");
                }

                seenBlocks[streamBlockIdx] = true;
            }
        }
    }

    private void checkDynamicHeader() {
        long lastHeaderEnd = footer.dataOffset + 512;
        long pos = footer.dataOffset;
        while (pos != -1) {
            if (pos % 512 != 0) {
                reportError("DynHeader: Unaligned header @%s", pos);
            }

            fileStream.position(pos);
            Header hdr = Header.fromStream(fileStream);
            if ( DynamicHeader.HeaderCookie.equals(hdr.cookie)) {
                if (dynamicHeader != null) {
                    reportError("DynHeader: Duplicate dynamic header found");
                }

                fileStream.position(pos);
                dynamicHeader = DynamicHeader.fromStream(fileStream);
                if (pos + 1024 > lastHeaderEnd) {
                    lastHeaderEnd = pos + 1024;
                }
            } else {
                reportWarning("DynHeader: Undocumented header found, with cookie '" + hdr.cookie + "'");
                if (pos + 512 > lastHeaderEnd) {
                    lastHeaderEnd = pos + 1024;
                }
            }
            pos = hdr.dataOffset;
        }
        if (dynamicHeader == null) {
            reportError("DynHeader: No dynamic header found");
            return;
        }

        if (dynamicHeader.tableOffset < lastHeaderEnd) {
            reportError("DynHeader: BAT offset is before last header");
        }

        if (dynamicHeader.tableOffset % 512 != 0) {
            reportError("DynHeader: BAT offset is not sector aligned");
        }

        if (dynamicHeader.headerVersion != 0x00010000) {
            reportError("DynHeader: Unrecognized header version");
        }

        if (dynamicHeader.maxTableEntries != MathUtilities.ceil(footer.currentSize, dynamicHeader.blockSize)) {
            reportError("DynHeader: Max table entries is invalid");
        }

        if ((dynamicHeader.blockSize != Sizes.OneMiB * 2) && (dynamicHeader.blockSize != Sizes.OneKiB * 512)) {
            reportWarning("DynHeader: Using non-standard block size '" + dynamicHeader.blockSize + "'");
        }

        if (!Utilities.isPowerOfTwo(dynamicHeader.blockSize)) {
            reportError("DynHeader: block size is not a power of 2");
        }

        if (!dynamicHeader.isChecksumValid()) {
            reportError("DynHeader: Invalid checksum");
        }

        if (footer.diskType == FileType.Dynamic && !dynamicHeader.parentUniqueId.equals(EMPTY)) {
            reportWarning("DynHeader: Parent Id is not null for dynamic disk");
        } else if (footer.diskType == FileType.Differencing && dynamicHeader.parentUniqueId.equals(EMPTY)) {
            reportError("DynHeader: Parent Id is null for differencing disk");
        }

        if (footer.diskType == FileType.Differencing && dynamicHeader.parentTimestamp > System.currentTimeMillis()) {
            reportWarning("DynHeader: Parent timestamp is greater than current time");
        }
    }

    private void checkFooterFields() {
        if (!"conectix".equals(footer.cookie)) {
            reportError("Footer: Invalid VHD cookie - should be 'connectix'");
        }

        if ((footer.features & ~1) != 2) {
            reportError("Footer: Invalid VHD features - should be 0x2 or 0x3");
        }

        if (footer.fileFormatVersion != 0x00010000) {
            reportError("Footer: Unrecognized VHD file version");
        }

        if (footer.diskType == FileType.Fixed && footer.dataOffset != -1) {
            reportError("Footer: Invalid data offset - should be 0xFFFFFFFF for fixed disks");
        } else if (footer.diskType != FileType.Fixed && (footer.dataOffset == 0 || footer.dataOffset == -1)) {
            reportError("Footer: Invalid data offset - should not be 0x0 or 0xFFFFFFFF for non-fixed disks");
        }

        if (footer.timestamp > System.currentTimeMillis()) {
            reportError("Footer: Invalid timestamp - creation time in file is greater than current time");
        }

        if (!"Wi2k".equals(footer.creatorHostOS) && !"Mac ".equals(footer.creatorHostOS)) {
            reportWarning("Footer: Creator Host OS is not a documented value ('Wi2K' or 'Mac '), is '" + footer.creatorHostOS +
                          "'");
        }

        if (footer.originalSize != footer.currentSize) {
            reportInfo("Footer: Current size of the disk doesn't match the original size");
        }

        if (footer.currentSize == 0) {
            reportError("Footer: Current size of the disk is 0 bytes");
        }

        if (!footer.geometry.equals(Geometry.fromCapacity(footer.currentSize))) {
            reportWarning("Footer: Disk Geometry does not match documented Microsoft geometry for this capacity");
        }

        if (footer.diskType != FileType.Fixed && footer.diskType != FileType.Dynamic &&
            footer.diskType != FileType.Differencing) {
            reportError("Footer: Undocumented disk type, not Fixed, Dynamic or Differencing");
        }

        if (!footer.isChecksumValid()) {
            reportError("Footer: Invalid footer checksum");
        }

        if (footer.uniqueId.equals(EMPTY)) {
            reportWarning("Footer: Unique Id is null");
        }
    }

    private void checkFooter() {
        fileStream.position(fileStream.getLength() - Sizes.Sector);
        byte[] sector = StreamUtilities.readExact(fileStream, Sizes.Sector);
        footer = Footer.fromBytes(sector, 0);
        if (!footer.isValid()) {
            reportError("Invalid VHD footer at end of file");
        }
    }

    private void checkHeader() {
        fileStream.position(0);
        byte[] headerSector = StreamUtilities.readExact(fileStream, Sizes.Sector);
        Footer header = Footer.fromBytes(headerSector, 0);
        if (!header.isValid()) {
            reportError("Invalid VHD footer at start of file");
        }

        fileStream.position(fileStream.getLength() - Sizes.Sector);
        byte[] footerSector = StreamUtilities.readExact(fileStream, Sizes.Sector);
        if (!Utilities.areEqual(footerSector, headerSector)) {
            reportError("Header and footer are different");
        }

        if (footer == null || !footer.isValid()) {
            footer = header;
        }
    }

    private void reportInfo(String str, Object... args) {
        levelsDetected.add(ReportLevels.Information);
        if (reportLevels.contains(ReportLevels.Information)) {
            report.printf("INFO: " + str, args);
        }
    }

    private void reportWarning(String str, Object... args) {
        levelsDetected.add(ReportLevels.Warnings);
        if (reportLevels.contains(ReportLevels.Warnings)) {
            report.printf("WARNING: " + str, args);
        }

    }

    private void reportError(String str, Object... args) {
        levelsDetected.add(ReportLevels.Errors);
        if (reportLevels.contains(ReportLevels.Errors)) {
            report.printf("ERROR: " + str, args);
        }
    }

    private final static class AbortException extends InvalidFileSystemException {
    }
}
