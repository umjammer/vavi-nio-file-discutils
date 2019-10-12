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

package DiscUtils.Fat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


public class DirectoryEntry {
    private FatType _fatVariant = FatType.None;

    private FatFileSystemOptions _options;

    private byte _attr;

    private short _creationDate;

    private short _creationTime;

    private byte _creationTimeTenth;

    private int _fileSize;

    private short _firstClusterHi;

    private short _firstClusterLo;

    private short _lastAccessDate;

    private short _lastWriteDate;

    private short _lastWriteTime;

    public DirectoryEntry(FatFileSystemOptions options, Stream stream, FatType fatVariant) {
        _options = options;
        _fatVariant = fatVariant;
        byte[] buffer = StreamUtilities.readExact(stream, 32);
        load(buffer, 0);
    }

    public DirectoryEntry(FatFileSystemOptions options, FileName name, EnumSet<FatAttributes> attrs, FatType fatVariant) {
        _options = options;
        _fatVariant = fatVariant;
        setName(name);
        _attr = (byte) FatAttributes.valueOf(attrs);
    }

    public DirectoryEntry(DirectoryEntry toCopy) {
        _options = toCopy._options;
        _fatVariant = toCopy._fatVariant;
        setName(toCopy.getName());
        _attr = toCopy._attr;
        _creationTimeTenth = toCopy._creationTimeTenth;
        _creationTime = toCopy._creationTime;
        _creationDate = toCopy._creationDate;
        _lastAccessDate = toCopy._lastAccessDate;
        _firstClusterHi = toCopy._firstClusterHi;
        _lastWriteTime = toCopy._lastWriteTime;
        _firstClusterLo = toCopy._firstClusterLo;
        _fileSize = toCopy._fileSize;
    }

    public EnumSet<FatAttributes> getAttributes() {
        return FatAttributes.valueOf(_attr);
    }

    public void setAttributes(EnumSet<FatAttributes> value) {
        _attr = (byte) FatAttributes.valueOf(value);
    }

    public long getCreationTime() {
        return fileTimeToDateTime(_creationDate, _creationTime, _creationTimeTenth);
    }

    public void setCreationTime(long value) {
        short[] refVar___0 = new short[1];
        short[] refVar___1 = new short[1];
        byte[] refVar___2 = new byte[1];
        dateTimeToFileTime(value, refVar___0, refVar___1, refVar___2);
        _creationDate = refVar___0[0];
        _creationTime = refVar___1[0];
        _creationTimeTenth = refVar___2[0];
    }

    public int getFileSize() {
        return _fileSize;
    }

    public void setFileSize(int value) {
        _fileSize = value;
    }

    public int getFirstCluster() {
        if (_fatVariant == FatType.Fat32) {
            return _firstClusterHi << 16 | _firstClusterLo;
        }

        return _firstClusterLo;
    }

    public void setFirstCluster(int value) {
        if (_fatVariant == FatType.Fat32) {
            _firstClusterHi = (short) ((value >>> 16) & 0xFFFF);
        }

        _firstClusterLo = (short) (value & 0xFFFF);
    }

    public long getLastAccessTime() {
        return fileTimeToDateTime(_lastAccessDate, (short) 0, (byte) 0);
    }

    public void setLastAccessTime(long value) {
        short[] date = new short[1];
        dateTimeToFileTime(value, date);
        _lastAccessDate = date[0];
    }

    public long getLastWriteTime() {
        return fileTimeToDateTime(_lastWriteDate, _lastWriteTime, (byte) 0);
    }

    public void setLastWriteTime(long value) {
        short[] date = new short[1];
        short[] time = new short[1];
        dateTimeToFileTime(value, date, time);
        _lastWriteDate = date[0];
        _lastWriteTime = time[0];
    }

    private FileName __Name;

    public FileName getName() {
        return __Name;
    }

    public void setName(FileName value) {
        __Name = value;
    }

    public void writeTo(Stream stream) {
        byte[] buffer = new byte[32];
        getName().getBytes(buffer, 0);
        buffer[11] = _attr;
        buffer[13] = _creationTimeTenth;
        EndianUtilities.writeBytesLittleEndian(_creationTime, buffer, 14);
        EndianUtilities.writeBytesLittleEndian(_creationDate, buffer, 16);
        EndianUtilities.writeBytesLittleEndian(_lastAccessDate, buffer, 18);
        EndianUtilities.writeBytesLittleEndian(_firstClusterHi, buffer, 20);
        EndianUtilities.writeBytesLittleEndian(_lastWriteTime, buffer, 22);
        EndianUtilities.writeBytesLittleEndian(_lastWriteDate, buffer, 24);
        EndianUtilities.writeBytesLittleEndian(_firstClusterLo, buffer, 26);
        EndianUtilities.writeBytesLittleEndian(_fileSize, buffer, 28);
        stream.write(buffer, 0, buffer.length);
    }

    private static long fileTimeToDateTime(short date, short time, byte tenths) {
        if (date == 0 || date == 0xFFFF) {
            return FatFileSystem.Epoch;
        }

        // Return Epoch - this is an invalid date
        int year = 1980 + ((date & 0xFE00) >>> 9);
        int month = (date & 0x01E0) >>> 5;
        int day = date & 0x001F;
        int hour = (time & 0xF800) >>> 11;
        int minute = (time & 0x07E0) >>> 5;
        int second = (time & 0x001F) * 2 + tenths / 100;
        int millis = tenths % 100 * 10;
        return LocalDateTime.of(year, month, day, hour, minute, second, millis * 1000).toEpochSecond(ZoneOffset.UTC); // TODO
    }

    private static void dateTimeToFileTime(long value, short[] date) {
        short[] time = new short[1];
        byte[] tenths = new byte[1];
        dateTimeToFileTime(value, date, time, tenths);
    }

    private static void dateTimeToFileTime(long value, short[] date, short[] time) {
        byte[] tenths = new byte[1];
        dateTimeToFileTime(value, date, time, tenths);
    }

    // TODO
    private static void dateTimeToFileTime(long value_, short[] date, short[] time, byte[] tenths) {
        LocalDateTime value = LocalDateTime.ofEpochSecond(value_, 0, ZoneOffset.UTC);
        if (value.getYear() < 1980) {
            value_ = FatFileSystem.Epoch;
        }

        date[0] = (short) ((((value.getYear() - 1980) << 9) & 0xFE00) | ((value.getMonthValue() << 5) & 0x01E0) |
                           (value.getDayOfMonth() & 0x001F));
        time[0] = (short) (((value.getHour() << 11) & 0xF800) | ((value.getMinute() << 5) & 0x07E0) |
                           ((value.getSecond() / 2) & 0x001F));
        tenths[0] = (byte) (value.getSecond() % 2 * 100 + value.getNano() * 100);
    }

    private void load(byte[] data, int offset) {
        setName(new FileName(data, offset));
        _attr = data[offset + 11];
        _creationTimeTenth = data[offset + 13];
        _creationTime = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 14);
        _creationDate = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 16);
        _lastAccessDate = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 18);
        _firstClusterHi = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 20);
        _lastWriteTime = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 22);
        _lastWriteDate = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 24);
        _firstClusterLo = (short) EndianUtilities.toUInt16LittleEndian(data, offset + 26);
        _fileSize = EndianUtilities.toUInt32LittleEndian(data, offset + 28);
    }
}
