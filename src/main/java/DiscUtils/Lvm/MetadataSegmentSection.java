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
import java.util.List;
import java.util.Scanner;

import dotnet4j.Tuple;


public class MetadataSegmentSection {
    public String Name;

    public long StartExtent;

    public long ExtentCount;

    public SegmentType Type = SegmentType.None;

    public long StripeCount;

    public List<MetadataStripe> Stripes;

    void parse(String head, Scanner data) {
        Name = head.trim().replaceFirst("\\{*$", "").replaceFirst(" *$", "");
        String line;
        while (data.hasNextLine()) {
            line = Metadata.readLine(data);
            if (line.equals(""))
                continue;
            if (line.contains("=")) {
                Tuple<String, String> parameter = Metadata.parseParameter(line);
                switch(parameter.getKey().trim().toLowerCase()) {
                case "start_extent":
                    StartExtent = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "extent_count":
                    ExtentCount = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "type":
                    String value = Metadata.parseStringValue(parameter.getValue());
                    switch (value) {
                    case "striped":
                        Type = SegmentType.Striped;
                        break;
                    case "zero":
                        Type = SegmentType.Zero;
                        break;
                    case "error":
                        Type = SegmentType.Error;
                        break;
                    case "free":
                        Type = SegmentType.Free;
                        break;
                    case "snapshot":
                        Type = SegmentType.Snapshot;
                        break;
                    case "mirror":
                        Type = SegmentType.Mirror;
                        break;
                    case "raid1":
                        Type = SegmentType.Raid1;
                        break;
                    case "raid10":
                        Type = SegmentType.Raid10;
                        break;
                    case "raid4":
                        Type = SegmentType.Raid4;
                        break;
                    case "raid5":
                        Type = SegmentType.Raid5;
                        break;
                    case "raid5_la":
                        Type = SegmentType.Raid5La;
                        break;
                    case "raid5_ra":
                        Type = SegmentType.Raid5Ra;
                        break;
                    case "raid5_ls":
                        Type = SegmentType.Raid5Ls;
                        break;
                    case "raid5_rs":
                        Type = SegmentType.Raid5Rs;
                        break;
                    case "raid6":
                        Type = SegmentType.Raid6;
                        break;
                    case "raid6_zr":
                        Type = SegmentType.Raid6Zr;
                        break;
                    case "raid6_nr":
                        Type = SegmentType.Raid6Nr;
                        break;
                    case "raid6_nc":
                        Type = SegmentType.Raid6Nc;
                        break;
                    case "thin-pool":
                        Type = SegmentType.ThinPool;
                        break;
                    case "thin":
                        Type = SegmentType.Thin;
                        break;
                    }
                    break;
                case "stripe_count":
                    StripeCount = Metadata.parseNumericValue(parameter.getValue());
                    break;
                case "stripes":
                    if (parameter.getValue().trim().equals("[")) {
                        Stripes = parseStripesSection(data);
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
            if (line.equals(""))
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
