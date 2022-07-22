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

package discUtils.fat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public class DirectoryEntry {

    private FatType fatVariant = FatType.None;

    private FatFileSystemOptions options;

    private byte attr;

    private short creationDate;

    private short creationTime;

    private byte creationTimeTenth;

    private int fileSize;

    private short firstClusterHi;

    private short firstClusterLo;

    private short lastAccessDate;

    private short lastWriteDate;

    private short lastWriteTime;

    public DirectoryEntry(FatFileSystemOptions options, Stream stream, FatType fatVariant) {
        this.options = options;
        this.fatVariant = fatVariant;
        byte[] buffer = StreamUtilities.readExact(stream, 32);
        load(buffer, 0);
    }

    public DirectoryEntry(FatFileSystemOptions options, FileName name, EnumSet<FatAttributes> attrs, FatType fatVariant) {
        this.options = options;
        this.fatVariant = fatVariant;
        this.name = name;
        attr = (byte) (FatAttributes.valueOf(attrs) & 0xff);
    }

    public DirectoryEntry(DirectoryEntry toCopy) {
        options = toCopy.options;
        fatVariant = toCopy.fatVariant;
        name = toCopy.getName();
        attr = toCopy.attr;
        creationTimeTenth = toCopy.creationTimeTenth;
        creationTime = toCopy.creationTime;
        creationDate = toCopy.creationDate;
        lastAccessDate = toCopy.lastAccessDate;
        firstClusterHi = toCopy.firstClusterHi;
        lastWriteTime = toCopy.lastWriteTime;
        firstClusterLo = toCopy.firstClusterLo;
        fileSize = toCopy.fileSize;
    }

    public EnumSet<FatAttributes> getAttributes() {
        return FatAttributes.valueOf(attr);
    }

    public void setAttributes(EnumSet<FatAttributes> value) {
        attr = (byte) FatAttributes.valueOf(value);
    }

    /**
     * @return epoch millis at system default zone
     */
    public long getCreationTime() {
        return fileTimeToDateTime(creationDate, creationTime, creationTimeTenth);
    }

    /**
     * @param value epoch millis at utc
     */
    public void setCreationTime(long value) {
        short[] creationDate = new short[1];
        short[] creationTime = new short[1];
        byte[] creationTimeTenth = new byte[1];
        dateTimeToFileTime(value, creationDate, creationTime, creationTimeTenth);
        this.creationDate = creationDate[0];
        this.creationTime = creationTime[0];
        this.creationTimeTenth = creationTimeTenth[0];
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int value) {
        fileSize = value;
    }

    public int getFirstCluster() {
        if (fatVariant == FatType.Fat32) {
            return (firstClusterHi & 0xffff) << 16 | (firstClusterLo & 0xffff);
        }

        return firstClusterLo;
    }

    public void setFirstCluster(int value) {
        if (fatVariant == FatType.Fat32) {
            firstClusterHi = (short) ((value >>> 16) & 0xFFFF);
        }

        firstClusterLo = (short) (value & 0xFFFF);
    }

    /**
     * @return epoch millis at system default zone
     */
    public long getLastAccessTime() {
        return fileTimeToDateTime(lastAccessDate, (short) 0, (byte) 0);
    }

    /**
     * @param value epoch millis at utc
     */
    public void setLastAccessTime(long value) {
        short[] date = new short[1];
        dateTimeToFileTime(value, date);
        lastAccessDate = date[0];
    }

    /**
     * @return epoch millis at system default zone
     */
    public long getLastWriteTime() {
        return fileTimeToDateTime(lastWriteDate, lastWriteTime, (byte) 0);
    }

    /**
     * @param value epoch millis at utc
     */
    public void setLastWriteTime(long value) {
        short[] date = new short[1];
        short[] time = new short[1];
        dateTimeToFileTime(value, date, time);
        lastWriteDate = date[0];
        lastWriteTime = time[0];
    }

    private FileName name;

    public FileName getName() {
        return name;
    }

    public void setName(FileName value) {
        name = value;
    }

    public void writeTo(Stream stream) {
        byte[] buffer = new byte[32];
        getName().getBytes(buffer, 0);
        buffer[11] = attr;
        buffer[13] = creationTimeTenth;
        EndianUtilities.writeBytesLittleEndian(creationTime, buffer, 14);
        EndianUtilities.writeBytesLittleEndian(creationDate, buffer, 16);
        EndianUtilities.writeBytesLittleEndian(lastAccessDate, buffer, 18);
        EndianUtilities.writeBytesLittleEndian(firstClusterHi, buffer, 20);
        EndianUtilities.writeBytesLittleEndian(lastWriteTime, buffer, 22);
        EndianUtilities.writeBytesLittleEndian(lastWriteDate, buffer, 24);
        EndianUtilities.writeBytesLittleEndian(firstClusterLo, buffer, 26);
        EndianUtilities.writeBytesLittleEndian(fileSize, buffer, 28);
        stream.write(buffer, 0, buffer.length);
    }

    /**
     * @return epoch millis at system default zone
     */
    private static long fileTimeToDateTime(short date, short time, byte tenths) {
        if (date == 0 || date == (short) 0xFFFF) {
            return FatFileSystem.Epoch;
        }

        // Return Epoch - this is an invalid date
        int year = 1980 + ((date & 0xFE00) >>> 9);
        int month = (date & 0x01E0) >>> 5;
        int day = date & 0x001F;
        int hour = (time & 0xF800) >>> 11;
        int minute = (time & 0x07E0) >>> 5;
        int second = (time & 0x001F) * 2 + (tenths & 0xff) / 100;
        int millis = (tenths & 0xff) % 100 * 10;
        return ZonedDateTime.of(year, month, day, hour, minute, second, millis, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * @param value epoch millis at utc
     * @param date {@cs out}
     */
    private static void dateTimeToFileTime(long value, short[] date) {
        short[] time = new short[1];
        byte[] tenths = new byte[1];
        dateTimeToFileTime(value, date, time, tenths);
    }

    /**
     * @param value epoch millis at utc
     * @param date {@cs out}
     * @param time {@cs out}
     */
    private static void dateTimeToFileTime(long value, short[] date, short[] time) {
        byte[] tenths = new byte[1];
        dateTimeToFileTime(value, date, time, tenths);
    }

    /**
     * @param value_ epoch millis at utc
     * @param date {@cs out}
     * @param time {@cs out}
     * @param tenths {@cs out}
     */
    private static void dateTimeToFileTime(long value_, short[] date, short[] time, byte[] tenths) {
        ZonedDateTime value = Instant.ofEpochMilli(value_).atZone(ZoneId.systemDefault());
        if (value.getYear() < 1980) {
            value = Instant.ofEpochMilli(FatFileSystem.Epoch).atZone(ZoneId.systemDefault());
        }

        date[0] = (short) ((((value.getYear() - 1980) << 9) & 0xFE00) | ((value.getMonthValue() << 5) & 0x01E0) |
                           (value.getDayOfMonth() & 0x001F));
        time[0] = (short) (((value.getHour() << 11) & 0xF800) | ((value.getMinute() << 5) & 0x07E0) |
                           ((value.getSecond() / 2) & 0x001F));
        tenths[0] = (byte) (value.getSecond() % 2 * 100 + value.getNano() * 100);
    }

    private void load(byte[] data, int offset) {
        name = new FileName(data, offset);
        attr = data[offset + 11];
        creationTimeTenth = data[offset + 13];
        creationTime = EndianUtilities.toUInt16LittleEndian(data, offset + 14);
        creationDate = EndianUtilities.toUInt16LittleEndian(data, offset + 16);
        lastAccessDate = EndianUtilities.toUInt16LittleEndian(data, offset + 18);
        firstClusterHi = EndianUtilities.toUInt16LittleEndian(data, offset + 20);
        lastWriteTime = EndianUtilities.toUInt16LittleEndian(data, offset + 22);
        lastWriteDate = EndianUtilities.toUInt16LittleEndian(data, offset + 24);
        firstClusterLo = EndianUtilities.toUInt16LittleEndian(data, offset + 26);
        fileSize = EndianUtilities.toUInt32LittleEndian(data, offset + 28);
    }
}
