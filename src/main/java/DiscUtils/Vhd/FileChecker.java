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

package DiscUtils.Vhd;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.UUID;

import DiscUtils.Core.Geometry;
import DiscUtils.Core.InvalidFileSystemException;
import DiscUtils.Core.ReportLevels;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * VHD file format verifier, that identifies corrupt VHD files.
 */
public class FileChecker {
    private final Stream _fileStream;

    private Footer _footer;

    private DynamicHeader _dynamicHeader;

    private PrintWriter _report;

    private EnumSet<ReportLevels> _reportLevels;

    private EnumSet<ReportLevels> _levelsDetected;

    private final ReportLevels _levelsConsideredFail = ReportLevels.Errors;

    /**
     * Initializes a new instance of the FileChecker class.
     *
     * @param stream The VHD file stream.
     */
    public FileChecker(Stream stream) {
        _fileStream = stream;
    }

    /**
     * Verifies the VHD file, generating a report and a pass/fail indication.
     *
     * @param reportOutput The destination for the report.
     * @param levels How verbose the report should be.
     * @return
     *         {@code true}
     *         if the file is valid, else false.
     */
    public boolean check(PrintWriter reportOutput, EnumSet<ReportLevels> levels) {
        _report = reportOutput;
        _reportLevels = levels;
        _levelsDetected = EnumSet.noneOf(ReportLevels.class);
        try {
            doCheck();
        } catch (AbortException ae) {
            reportError("File system check aborted: " + ae);
            return false;
        } catch (Exception e) {
            reportError("File system check aborted with exception: " + e);
            return false;
        }

        return !_levelsDetected.contains(_levelsConsideredFail);
    }

    private static void abort() throws IOException {
        throw new AbortException();
    }

    private void doCheck() throws IOException {
        checkFooter();
        if (_footer == null || _footer.DiskType != FileType.Fixed) {
            checkHeader();
        }

        if (_footer == null) {
            reportError("Unable to continue - no valid header or footer");
            abort();
        }

        checkFooterFields();
        if (_footer.DiskType != FileType.Fixed) {
            checkDynamicHeader();
            checkBat();
        }

    }

    private void checkBat() {
        int batSize = MathUtilities.roundUp(_dynamicHeader.MaxTableEntries * 4, Sizes.Sector);
        if (_dynamicHeader.TableOffset > _fileStream.getLength() - batSize) {
            reportError("BAT: BAT extends beyond end of file");
            return;
        }

        _fileStream.setPosition(_dynamicHeader.TableOffset);
        byte[] batData = StreamUtilities.readExact(_fileStream, batSize);
        int[] bat = new int[batSize / 4];
        for (int i = 0; i < bat.length; ++i) {
            bat[i] = EndianUtilities.toUInt32BigEndian(batData, i * 4);
        }
        for (int i = _dynamicHeader.MaxTableEntries; i < bat.length; ++i) {
            if (bat[i] != Integer.MAX_VALUE) {
                reportError("BAT: Padding record '" + i + "' should be 0xFFFFFFFF");
            }

        }
        int dataStartSector = Integer.MAX_VALUE;
        for (int i = 0; i < _dynamicHeader.MaxTableEntries; ++i) {
            if (bat[i] < dataStartSector) {
                dataStartSector = bat[i];
            }

        }
        if (dataStartSector == Integer.MAX_VALUE) {
            return;
        }

        long dataStart = (long) dataStartSector * Sizes.Sector;
        int blockBitmapSize = MathUtilities.roundUp(_dynamicHeader.BlockSize / Sizes.Sector / 8, Sizes.Sector);
        int storedBlockSize = _dynamicHeader.BlockSize + blockBitmapSize;
        boolean[] seenBlocks = new boolean[_dynamicHeader.MaxTableEntries];
        for (int i = 0; i < _dynamicHeader.MaxTableEntries; ++i) {
            if (bat[i] != Integer.MAX_VALUE) {
                long absPos = (long) bat[i] * Sizes.Sector;
                if (absPos + storedBlockSize > _fileStream.getLength()) {
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
        long lastHeaderEnd = _footer.DataOffset + 512;
        long pos = _footer.DataOffset;
        while (pos != -1) {
            if (pos % 512 != 0) {
                reportError("DynHeader: Unaligned header @{0}", pos);
            }

            _fileStream.setPosition(pos);
            Header hdr = Header.fromStream(_fileStream);
            if ( DynamicHeader.HeaderCookie.equals(hdr.Cookie)) {
                if (_dynamicHeader != null) {
                    reportError("DynHeader: Duplicate dynamic header found");
                }

                _fileStream.setPosition(pos);
                _dynamicHeader = DynamicHeader.fromStream(_fileStream);
                if (pos + 1024 > lastHeaderEnd) {
                    lastHeaderEnd = pos + 1024;
                }

            } else {
                reportWarning("DynHeader: Undocumented header found, with cookie '" + hdr.Cookie + "'");
                if (pos + 512 > lastHeaderEnd) {
                    lastHeaderEnd = pos + 1024;
                }

            }
            pos = hdr.DataOffset;
        }
        if (_dynamicHeader == null) {
            reportError("DynHeader: No dynamic header found");
            return;
        }

        if (_dynamicHeader.TableOffset < lastHeaderEnd) {
            reportError("DynHeader: BAT offset is before last header");
        }

        if (_dynamicHeader.TableOffset % 512 != 0) {
            reportError("DynHeader: BAT offset is not sector aligned");
        }

        if (_dynamicHeader.HeaderVersion != 0x00010000) {
            reportError("DynHeader: Unrecognized header version");
        }

        if (_dynamicHeader.MaxTableEntries != MathUtilities.ceil(_footer.CurrentSize, _dynamicHeader.BlockSize)) {
            reportError("DynHeader: Max table entries is invalid");
        }

        if ((_dynamicHeader.BlockSize != Sizes.OneMiB * 2) && (_dynamicHeader.BlockSize != Sizes.OneKiB * 512)) {
            reportWarning("DynHeader: Using non-standard block size '" + _dynamicHeader.BlockSize + "'");
        }

        if (!Utilities.isPowerOfTwo(_dynamicHeader.BlockSize)) {
            reportError("DynHeader: Block size is not a power of 2");
        }

        if (!_dynamicHeader.isChecksumValid()) {
            reportError("DynHeader: Invalid checksum");
        }

        if (_footer.DiskType == FileType.Dynamic && _dynamicHeader.ParentUniqueId != new UUID(0L, 0L)) {
            reportWarning("DynHeader: Parent Id is not null for dynamic disk");
        } else if (_footer.DiskType == FileType.Differencing && _dynamicHeader.ParentUniqueId == new UUID(0L, 0L)) {
            reportError("DynHeader: Parent Id is null for differencing disk");
        }

        if (_footer.DiskType == FileType.Differencing && _dynamicHeader.ParentTimestamp > System.currentTimeMillis()) {
            reportWarning("DynHeader: Parent timestamp is greater than current time");
        }

    }

    private void checkFooterFields() {
        if (!"conectix".equals(_footer.Cookie)) {
            reportError("Footer: Invalid VHD cookie - should be 'connectix'");
        }

        if ((_footer.Features & ~1) != 2) {
            reportError("Footer: Invalid VHD features - should be 0x2 or 0x3");
        }

        if (_footer.FileFormatVersion != 0x00010000) {
            reportError("Footer: Unrecognized VHD file version");
        }

        if (_footer.DiskType == FileType.Fixed && _footer.DataOffset != -1) {
            reportError("Footer: Invalid data offset - should be 0xFFFFFFFF for fixed disks");
        } else if (_footer.DiskType != FileType.Fixed && (_footer.DataOffset == 0 || _footer.DataOffset == -1)) {
            reportError("Footer: Invalid data offset - should not be 0x0 or 0xFFFFFFFF for non-fixed disks");
        }

        if (_footer.Timestamp > System.currentTimeMillis()) {
            reportError("Footer: Invalid timestamp - creation time in file is greater than current time");
        }

        if (!"Wi2k".equals(_footer.CreatorHostOS) && !"Mac ".equals(_footer.CreatorHostOS)) {
            reportWarning("Footer: Creator Host OS is not a documented value ('Wi2K' or 'Mac '), is '" + _footer.CreatorHostOS +
                          "'");
        }

        if (_footer.OriginalSize != _footer.CurrentSize) {
            reportInfo("Footer: Current size of the disk doesn't match the original size");
        }

        if (_footer.CurrentSize == 0) {
            reportError("Footer: Current size of the disk is 0 bytes");
        }

        if (!_footer.Geometry.equals(Geometry.fromCapacity(_footer.CurrentSize))) {
            reportWarning("Footer: Disk Geometry does not match documented Microsoft geometry for this capacity");
        }

        if (_footer.DiskType != FileType.Fixed && _footer.DiskType != FileType.Dynamic &&
            _footer.DiskType != FileType.Differencing) {
            reportError("Footer: Undocumented disk type, not Fixed, Dynamic or Differencing");
        }

        if (!_footer.isChecksumValid()) {
            reportError("Footer: Invalid footer checksum");
        }

        if (_footer.UniqueId == new UUID(0L, 0L)) {
            reportWarning("Footer: Unique Id is null");
        }

    }

    private void checkFooter() {
        _fileStream.setPosition(_fileStream.getLength() - Sizes.Sector);
        byte[] sector = StreamUtilities.readExact(_fileStream, Sizes.Sector);
        _footer = Footer.fromBytes(sector, 0);
        if (!_footer.isValid()) {
            reportError("Invalid VHD footer at end of file");
        }

    }

    private void checkHeader() {
        _fileStream.setPosition(0);
        byte[] headerSector = StreamUtilities.readExact(_fileStream, Sizes.Sector);
        Footer header = Footer.fromBytes(headerSector, 0);
        if (!header.isValid()) {
            reportError("Invalid VHD footer at start of file");
        }

        _fileStream.setPosition(_fileStream.getLength() - Sizes.Sector);
        byte[] footerSector = StreamUtilities.readExact(_fileStream, Sizes.Sector);
        if (!Utilities.areEqual(footerSector, headerSector)) {
            reportError("Header and footer are different");
        }

        if (_footer == null || !_footer.isValid()) {
            _footer = header;
        }

    }

    private void reportInfo(String str, Object... args) {
        _levelsDetected.add(ReportLevels.Information);
        if (_reportLevels.contains(ReportLevels.Information)) {
            _report.printf("INFO: " + str, args);
        }

    }

    private void reportWarning(String str, Object... args) {
        _levelsDetected.add(ReportLevels.Warnings);
        if (_reportLevels.contains(ReportLevels.Warnings)) {
            _report.printf("WARNING: " + str, args);
        }

    }

    private void reportError(String str, Object... args) {
        _levelsDetected.add(ReportLevels.Errors);
        if (_reportLevels.contains(ReportLevels.Errors)) {
            _report.printf("ERROR: " + str, args);
        }

    }

    private final static class AbortException extends InvalidFileSystemException {
    }

}
