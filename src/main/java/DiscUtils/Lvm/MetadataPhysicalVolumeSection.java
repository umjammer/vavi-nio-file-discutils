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

import java.util.EnumSet;
import java.util.Scanner;

import dotnet4j.Tuple;


public class MetadataPhysicalVolumeSection {
    public String Name;

    public String Id;

    public String Device;

    public EnumSet<PhysicalVolumeStatus> Status = EnumSet.noneOf(PhysicalVolumeStatus.class);

    public String[] Flags;

    public long DeviceSize;

    public long PeStart;

    public long PeCount;

    public void parse(String head, Scanner data) {
        Name = head.trim().replaceFirst("\\{*$", "").replaceFirst(" *$", "");
        String line;
        while ((line = Metadata.readLine(data)) != null) {
            if (line.isEmpty())
                continue;

            if (line.contains("=")) {
                Tuple<String, String> parameter = Metadata.parseParameter(line);
                String paramValue = parameter.getKey().trim().toLowerCase();
                if (paramValue.equals("id")) {
                    Id = Metadata.parseStringValue(parameter.getValue());
                } else if (paramValue.equals("device")) {
                    Device = Metadata.parseStringValue(parameter.getValue());
                } else if (paramValue.equals("status")) {
                    String[] values = Metadata.parseArrayValue(parameter.getValue());
                    for (String value : values) {
                        String statusValue = value.toLowerCase().trim();
                        if (statusValue.equals("read")) {
                            Status.add(PhysicalVolumeStatus.Read);
                        } else if (statusValue.equals("write")) {
                            Status.add(PhysicalVolumeStatus.Write);
                        } else if (statusValue.equals("allocatable")) {
                            Status.add(PhysicalVolumeStatus.Allocatable);
                        } else {
                            throw new IndexOutOfBoundsException("Unexpected status in physical volume metadata");
                        }
                    }
                } else if (paramValue.equals("flags")) {
                    Flags = Metadata.parseArrayValue(parameter.getValue());
                } else if (paramValue.equals("dev_size")) {
                    DeviceSize = Metadata.parseNumericValue(parameter.getValue());
                } else if (paramValue.equals("pe_start")) {
                    PeStart = Metadata.parseNumericValue(parameter.getValue());
                } else if (paramValue.equals("pe_count")) {
                    PeCount = Metadata.parseNumericValue(parameter.getValue());
                } else {
                    throw new IndexOutOfBoundsException("Unexpected parameter in global metadata");
                }
            } else if (line.endsWith("}")) {
                break;
            } else {
                throw new IndexOutOfBoundsException("unexpected input");
            }
        }
    }
}

enum PhysicalVolumeStatus {
    None(0x0),
    Read(0x1),
    Write(0x4),
    Allocatable(0x8);

    private int value;

    public int getValue() {
        return value;
    }

    private PhysicalVolumeStatus(int value) {
        this.value = value;
    }
}
