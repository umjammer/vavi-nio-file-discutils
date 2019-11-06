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
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;

import dotnet4j.Tuple;


public class MetadataVolumeGroupSection {
    public String Name;

    public String Id;

    public long SequenceNumber;

    public String Format;

    public EnumSet<VolumeGroupStatus> Status = EnumSet.noneOf(VolumeGroupStatus.class);

    public String[] Flags;

    public long ExtentSize;

    public long MaxLv;

    public long MaxPv;

    public long MetadataCopies;

    public List<MetadataPhysicalVolumeSection> PhysicalVolumes;

    public List<MetadataLogicalVolumeSection> LogicalVolumes;

    public void parse(String head, Scanner data) {
        Name = head.trim().replaceFirst("\\{*$", "").replaceFirst(" *$", "");
        String line;
        while (data.hasNextLine()) {
            line = Metadata.readLine(data);
            if (line.equals(""))
                continue;

            if (line.contains("=")) {
                Tuple<String, String> parameter = Metadata.parseParameter(line);
                String paramValue = parameter.getKey().trim().toLowerCase();
                if (paramValue.equals("id")) {
                    Id = Metadata.parseStringValue(parameter.getValue());
                } else if (paramValue.equals("seqno")) {
                    SequenceNumber = Metadata.parseNumericValue(parameter.getValue());
                } else if (paramValue.equals("format")) {
                    Format = Metadata.parseStringValue(parameter.getValue());
                } else if (paramValue.equals("status")) {
                    String[] values = Metadata.parseArrayValue(parameter.getValue());
                    for (String value : values) {
                        String statusValue = value.toLowerCase().trim();
                        if (statusValue.equals("read")) {
                            Status.add(VolumeGroupStatus.Read);
                        } else if (statusValue.equals("write")) {
                            Status.add(VolumeGroupStatus.Write);
                        } else if (statusValue.equals("resizeable")) {
                            Status.add(VolumeGroupStatus.Resizeable);
                        } else {
                            throw new IllegalArgumentException("Unexpected status in volume group metadata: " + statusValue);
                        }
                    }
                } else if (paramValue.equals("flags")) {
                    Flags = Metadata.parseArrayValue(parameter.getValue());
                } else if (paramValue.equals("extent_size")) {
                    ExtentSize = Metadata.parseNumericValue(parameter.getValue());
                } else if (paramValue.equals("max_lv")) {
                    MaxLv = Metadata.parseNumericValue(parameter.getValue());
                } else if (paramValue.equals("max_pv")) {
                    MaxPv = Metadata.parseNumericValue(parameter.getValue());
                } else if (paramValue.equals("metadata_copies")) {
                    MetadataCopies = Metadata.parseNumericValue(parameter.getValue());
                } else {
                    throw new IllegalArgumentException("Unexpected parameter in volume group metadata: " + parameter.getKey());
                }
            } else if (line.endsWith("{")) {
                String sectionName = line.replaceFirst("\\{*$", "").replaceFirst(" *$", "").toLowerCase();
                if (sectionName.equals("physical_volumes")) {
                    PhysicalVolumes = parsePhysicalVolumeSection(data);
                } else if (sectionName.equals("logical_volumes")) {
                    LogicalVolumes = parseLogicalVolumeSection(data);
                } else {
                    throw new IllegalArgumentException("Unexpected section in volume group metadata: " + sectionName);
                }
            } else if (line.endsWith("}")) {
                break;
            }
        }
    }

    private List<MetadataLogicalVolumeSection> parseLogicalVolumeSection(Scanner data) {
        List<MetadataLogicalVolumeSection> result = new ArrayList<>();
        String line;
        while (data.hasNextLine()) {
            line = Metadata.readLine(data);
            if (line.equals(""))
                continue;
            if (line.endsWith("{")) {
                MetadataLogicalVolumeSection pv = new MetadataLogicalVolumeSection();
                pv.parse(line, data);
                result.add(pv);
            } else if (line.endsWith("}")) {
                break;
            }
        }
        return result;
    }

    private List<MetadataPhysicalVolumeSection> parsePhysicalVolumeSection(Scanner data) {
        List<MetadataPhysicalVolumeSection> result = new ArrayList<>();
        String line;
        while (data.hasNextLine()) {
            line = Metadata.readLine(data);
            if (line.equals(""))
                continue;
            if (line.endsWith("{")) {
                MetadataPhysicalVolumeSection pv = new MetadataPhysicalVolumeSection();
                pv.parse(line, data);
                result.add(pv);
            } else if (line.endsWith("}")) {
                break;
            }
        }
        return result;
    }
}

enum VolumeGroupStatus {
    None(0x0),
    Read(0x1),
    Write(0x2),
    Resizeable(0x4);

    private int value;

    public int getValue() {
        return value;
    }

    private VolumeGroupStatus(int value) {
        this.value = value;
    }
}
