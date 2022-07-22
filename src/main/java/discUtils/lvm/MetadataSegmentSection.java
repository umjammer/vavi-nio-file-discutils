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
import java.util.List;
import java.util.Scanner;

import dotnet4j.util.compat.Tuple;


public class MetadataSegmentSection {

    public String name;

    public long startExtent;

    public long extentCount;

    public SegmentType type = SegmentType.None;

    public long stripeCount;

    public List<MetadataStripe> stripes;

    void parse(String head, Scanner data) {
        name = head.trim().replaceFirst("\\{*$", "").replaceFirst(" *$", "");
        String line;
        while (data.hasNextLine()) {
            line = Metadata.readLine(data);
            if (line.isEmpty())
                continue;
            if (line.contains("=")) {
                Tuple<String, String> parameter = Metadata.parseParameter(line);
                switch(parameter.getKey().trim().toLowerCase()) {
                case "start_extent":
                    startExtent = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "extent_count":
                    extentCount = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "type":
                    String value = Metadata.parseStringValue(parameter.getValue());
                    switch (value) {
                    case "striped":
                        type = SegmentType.Striped;
                        break;
                    case "zero":
                        type = SegmentType.Zero;
                        break;
                    case "error":
                        type = SegmentType.Error;
                        break;
                    case "free":
                        type = SegmentType.Free;
                        break;
                    case "snapshot":
                        type = SegmentType.Snapshot;
                        break;
                    case "mirror":
                        type = SegmentType.Mirror;
                        break;
                    case "raid1":
                        type = SegmentType.Raid1;
                        break;
                    case "raid10":
                        type = SegmentType.Raid10;
                        break;
                    case "raid4":
                        type = SegmentType.Raid4;
                        break;
                    case "raid5":
                        type = SegmentType.Raid5;
                        break;
                    case "raid5_la":
                        type = SegmentType.Raid5La;
                        break;
                    case "raid5_ra":
                        type = SegmentType.Raid5Ra;
                        break;
                    case "raid5_ls":
                        type = SegmentType.Raid5Ls;
                        break;
                    case "raid5_rs":
                        type = SegmentType.Raid5Rs;
                        break;
                    case "raid6":
                        type = SegmentType.Raid6;
                        break;
                    case "raid6_zr":
                        type = SegmentType.Raid6Zr;
                        break;
                    case "raid6_nr":
                        type = SegmentType.Raid6Nr;
                        break;
                    case "raid6_nc":
                        type = SegmentType.Raid6Nc;
                        break;
                    case "thin-pool":
                        type = SegmentType.ThinPool;
                        break;
                    case "thin":
                        type = SegmentType.Thin;
                        break;
                    }
                    break;
                case "stripe_count":
                    stripeCount = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "stripes":
                    if (parameter.getValue().trim().equals("[")) {
                        stripes = parseStripesSection(data);
                    }
                    break;
                default:
                    throw new IndexOutOfBoundsException("Unexpected parameter in global metadata: " + parameter.getKey());
                }
            } else if (line.endsWith("}")) {
                return;
            } else {
                throw new IndexOutOfBoundsException("unexpected input: " + line);
            }
        }
    }

    private List<MetadataStripe> parseStripesSection(Scanner data) {
        List<MetadataStripe> result = new ArrayList<>();
        String line;
        while (data.hasNextLine()) {
            line = Metadata.readLine(data);
            if (line.isEmpty())
                continue;
            if (line.endsWith("]")) {
                return result;
            }
            MetadataStripe pv = new MetadataStripe();
            pv.parse(line);
            result.add(pv);
        }
        return result;
    }
}

enum SegmentType {
    //$ lvm segtypes, man(8) lvm
    None,
    Striped,
    Zero,
    Error,
    Free,
    Snapshot,
    Mirror,
    Raid1,
    Raid10,
    Raid4,
    Raid5,
    Raid5La,
    Raid5Ra,
    Raid5Ls,
    Raid5Rs,
    Raid6,
    Raid6Zr,
    Raid6Nr,
    Raid6Nc,
    ThinPool,
    Thin
}
