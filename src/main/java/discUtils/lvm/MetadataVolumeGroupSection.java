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

package discUtils.lvm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;

import dotnet4j.util.compat.Tuple;


public class MetadataVolumeGroupSection {

    public String name;

    public String id;

    public long sequenceNumber;

    public String format;

    public EnumSet<VolumeGroupStatus> status = EnumSet.noneOf(VolumeGroupStatus.class);

    public String[] flags;

    public long extentSize;

    public long maxLv;

    public long maxPv;

    public long metadataCopies;

    public List<MetadataPhysicalVolumeSection> physicalVolumes;

    public List<MetadataLogicalVolumeSection> logicalVolumes;

    public void parse(String head, Scanner data) {
        name = head.trim().replaceFirst("\\{*$", "").replaceFirst(" *$", "");
        String line;
        while (data.hasNextLine()) {
            line = Metadata.readLine(data);
            if (line.equals(""))
                continue;

            if (line.contains("=")) {
                Tuple<String, String> parameter = Metadata.parseParameter(line);
                String paramValue = parameter.getKey().trim().toLowerCase();
                switch (paramValue) {
                case "id":
                    id = Metadata.parseStringValue(parameter.getValue());
                    break;
                case "seqno":
                    sequenceNumber = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "format":
                    format = Metadata.parseStringValue(parameter.getValue());
                    break;
                case "status":
                    String[] values = Metadata.parseArrayValue(parameter.getValue());
                    for (String value : values) {
                        String statusValue = value.toLowerCase().trim();
                        switch (statusValue) {
                        case "read":
                            status.add(VolumeGroupStatus.Read);
                            break;
                        case "write":
                            status.add(VolumeGroupStatus.Write);
                            break;
                        case "resizeable":
                            status.add(VolumeGroupStatus.Resizeable);
                            break;
                        default:
                            throw new IllegalArgumentException("Unexpected status in volume group metadata: " + statusValue);
                        }
                    }
                    break;
                case "flags":
                    flags = Metadata.parseArrayValue(parameter.getValue());
                    break;
                case "extent_size":
                    extentSize = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "max_lv":
                    maxLv = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "max_pv":
                    maxPv = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "metadata_copies":
                    metadataCopies = Metadata.parseNumericValue(parameter.getValue());
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected parameter in volume group metadata: " + parameter.getKey());
                }
            } else if (line.endsWith("{")) {
                String sectionName = line.replaceFirst("\\{*$", "").replaceFirst(" *$", "").toLowerCase();
                if (sectionName.equals("physical_volumes")) {
                    physicalVolumes = parsePhysicalVolumeSection(data);
                } else if (sectionName.equals("logical_volumes")) {
                    logicalVolumes = parseLogicalVolumeSection(data);
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

    private final int value;

    public int getValue() {
        return value;
    }

    VolumeGroupStatus(int value) {
        this.value = value;
    }
}
