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

import java.util.EnumSet;
import java.util.Scanner;

import dotnet4j.util.compat.Tuple;


public class MetadataPhysicalVolumeSection {

    public String name;

    public String id;

    public String device;

    public EnumSet<PhysicalVolumeStatus> status = EnumSet.noneOf(PhysicalVolumeStatus.class);

    public String[] flags;

    public long deviceSize;

    public long peStart;

    public long peCount;

    public void parse(String head, Scanner data) {
        name = head.trim().replaceFirst("\\{*$", "").replaceFirst(" *$", "");
        String line;
        while ((line = Metadata.readLine(data)) != null) {
            if (line.isEmpty())
                continue;

            if (line.contains("=")) {
                Tuple<String, String> parameter = Metadata.parseParameter(line);
                String paramValue = parameter.getKey().trim().toLowerCase();
                switch (paramValue) {
                case "id":
                    id = Metadata.parseStringValue(parameter.getValue());
                    break;
                case "device":
                    device = Metadata.parseStringValue(parameter.getValue());
                    break;
                case "status":
                    String[] values = Metadata.parseArrayValue(parameter.getValue());
                    for (String value : values) {
                        String statusValue = value.toLowerCase().trim();
                        switch (statusValue) {
                        case "read":
                            status.add(PhysicalVolumeStatus.Read);
                            break;
                        case "write":
                            status.add(PhysicalVolumeStatus.Write);
                            break;
                        case "allocatable":
                            status.add(PhysicalVolumeStatus.Allocatable);
                            break;
                        default:
                            throw new IndexOutOfBoundsException("Unexpected status in physical volume metadata");
                        }
                    }
                    break;
                case "flags":
                    flags = Metadata.parseArrayValue(parameter.getValue());
                    break;
                case "dev_size":
                    deviceSize = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "pe_start":
                    peStart = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "pe_count":
                    peCount = Metadata.parseNumericValue(parameter.getValue());
                    break;
                default:
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

    private final int
            value;

    public int getValue() {
        return value;
    }

    PhysicalVolumeStatus(int value) {
        this.value = value;
    }
}
