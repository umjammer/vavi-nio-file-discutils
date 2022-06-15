//
// Copyright (c) 2016, Bianco Veigel
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

package DiscUtils.Lvm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import DiscUtils.Streams.ConcatStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SparseStreamOpenDelegate;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.Tuple;


public class MetadataLogicalVolumeSection {
    public String Name;

    public String Id;

    public UUID Identity;

    public EnumSet<LogicalVolumeStatus> Status = EnumSet.noneOf(LogicalVolumeStatus.class);

    public String[] Flags;

    public String CreationHost;

    public long CreationTime;

    public long SegmentCount;

    public List<MetadataSegmentSection> Segments;

    private Map<String, PhysicalVolume> _pvs;

    private long _extentSize;

    public void parse(String head, Scanner data) {
        List<MetadataSegmentSection> segments = new ArrayList<>();
        Name = head.trim().replaceFirst("\\{*$", "").replaceFirst(" *$", "");
        String line;
        while ((line = Metadata.readLine(data)) != null) {
            if (line.equals(""))
                continue;

            if (line.contains("=")) {
                Tuple<String, String> parameter = Metadata.parseParameter(line);
                String paramValue = parameter.getKey().trim().toLowerCase();
                switch (paramValue) {
                case "id":
                    Id = Metadata.parseStringValue(parameter.getValue());
                    byte[] guid = new byte[16];
                    EndianUtilities.stringToBytes(Id.replace("-", ""), guid, 0, 16);
                    // Mark it as a version 4 GUID
                    guid[7] = (byte) ((guid[7] | (byte) 0x40) & (byte) 0x4f);
                    guid[8] = (byte) ((guid[8] | (byte) 0x80) & (byte) 0xbf);
                    Identity = UUID.nameUUIDFromBytes(guid);
                    break;
                case "status":
                    String[] values = Metadata.parseArrayValue(parameter.getValue());
                    for (String value : values) {
                        String statusValue = value.toLowerCase().trim();
                        if (statusValue.equals("read")) {
                            Status.add(LogicalVolumeStatus.Read);
                        } else if (statusValue.equals("write")) {
                            Status.add(LogicalVolumeStatus.Write);
                        } else if (statusValue.equals("visible")) {
                            Status.add(LogicalVolumeStatus.Visible);
                        } else {
                            throw new IndexOutOfBoundsException("Unexpected status in physical volume metadata");
                        }
                    }
                    break;
                case "flags":
                    Flags = Metadata.parseArrayValue(parameter.getValue());
                    break;
                case "creation_host":
                    CreationHost = Metadata.parseStringValue(parameter.getValue());
                    break;
                case "creation_time":
                    CreationTime = Metadata.parseDateTimeValue(parameter.getValue());
                    break;
                case "segment_count":
                    SegmentCount = Metadata.parseNumericValue(parameter.getValue());
                    break;
                default:
                    throw new IndexOutOfBoundsException("Unexpected parameter in global metadata " + parameter.getKey());
                }
            } else if (line.endsWith("{")) {
                MetadataSegmentSection segment = new MetadataSegmentSection();
                segment.parse(line, data);
                segments.add(segment);
            } else if (line.endsWith("}")) {
                break;
            } else {
                throw new IndexOutOfBoundsException("unexpected input " + line);
            }
        }
        Segments = segments;
    }

    public long getExtentCount() {
        long length = 0L;
        for (MetadataSegmentSection segment : Segments) {
            length += segment.ExtentCount;
        }
        return length;
    }

    public SparseStreamOpenDelegate open(Map<String, PhysicalVolume> availablePvs, long extentSize) {
        _pvs = availablePvs;
        _extentSize = extentSize;
        return this::open;
    }

    private SparseStream open() {
        if (!Status.contains(LogicalVolumeStatus.Read))
            throw new dotnet4j.io.IOException("volume is not readable");

        List<MetadataSegmentSection> segments = new ArrayList<>();
        for (MetadataSegmentSection segment : Segments) {
            if (segment.Type != SegmentType.Striped)
                throw new dotnet4j.io.IOException("unsupported segment type");

            segments.add(segment);
        }
        segments.sort(compareSegments);
        // Sanity Check...
        long pos = 0;
        for (MetadataSegmentSection segment : segments) {
            if (segment.StartExtent != pos) {
                throw new dotnet4j.io.IOException("Volume extents are non-contiguous");
            }

            pos += segment.ExtentCount;
        }
        List<SparseStream> streams = new ArrayList<>();
        for (MetadataSegmentSection segment : segments) {
            streams.add(openSegment(segment));
        }
        return new ConcatStream(Ownership.Dispose, streams);
    }

    private SparseStream openSegment(MetadataSegmentSection segment) {
        if (segment.Stripes.size() != 1) {
            throw new dotnet4j.io.IOException("invalid number of stripes");
        }

        MetadataStripe stripe = segment.Stripes.get(0);
        PhysicalVolume pv;
        if (!_pvs.containsKey(stripe.PhysicalVolumeName)) {
            throw new dotnet4j.io.IOException("missing pv");
        }
        pv = _pvs.get(stripe.PhysicalVolumeName);

        if (pv.PvHeader.DiskAreas.size() != 1) {
            throw new dotnet4j.io.IOException("invalid number od pv data areas");
        }

        DiskArea dataArea = pv.PvHeader.DiskAreas.get(0);
        long start = dataArea.Offset + (stripe.StartExtentNumber * _extentSize * PhysicalVolume.SECTOR_SIZE);
        long length = segment.ExtentCount * _extentSize * PhysicalVolume.SECTOR_SIZE;
        return new SubStream(pv.getContent(), Ownership.None, start, length);
    }

    private Comparator<MetadataSegmentSection> compareSegments = (x, y) -> {
        if (x.StartExtent > y.StartExtent) {
            return 1;
        } else if (x.StartExtent < y.StartExtent) {
            return -1;
        }

        return 0;
    };
}

enum LogicalVolumeStatus {
    None(0x0),
    Read(0x1),
    Write(0x2),
    Visible(0x4);

    private final int value;

    public int getValue() {
        return value;
    }

    LogicalVolumeStatus(int value) {
        this.value = value;
    }
}
