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

import DiscUtils.Core.CoreCompat.Tuple;


public class MetadataSegmentSection {
    public String Name;

    public long StartExtent;

    public long ExtentCount;

    public SegmentType Type = SegmentType.None;

    public long StripeCount;

    public List<MetadataStripe> Stripes;

    public void parse(String head, Scanner data) {
        Name = head.trim().replaceFirst("\\{*$", "").replaceFirst(" *$", "");
        String line;
        while ((line = Metadata.readLine(data)) != null) {
            if (line.equals(""))
                continue;

            if (line.contains("=")) {
                Tuple<String, String> parameter = Metadata.parseParameter(line);
                String key = parameter.getKey().trim().toLowerCase();
                if (key.equals("start_extent")) {
                    StartExtent = Metadata.parseNumericValue(parameter.getValue());
                } else if (key.equals("extent_count")) {
                    ExtentCount = Metadata.parseNumericValue(parameter.getValue());
                } else if (key.equals("type")) {
                    String value = Metadata.parseStringValue(parameter.getValue());
                    if (value.equals("striped")) {
                        Type = SegmentType.Striped;
                    } else if (value.equals("zero")) {
                        Type = SegmentType.Zero;
                    } else if (value.equals("error")) {
                        Type = SegmentType.Error;
                    } else if (value.equals("free")) {
                        Type = SegmentType.Free;
                    } else if (value.equals("snapshot")) {
                        Type = SegmentType.Snapshot;
                    } else if (value.equals("mirror")) {
                        Type = SegmentType.Mirror;
                    } else if (value.equals("raid1")) {
                        Type = SegmentType.Raid1;
                    } else if (value.equals("raid10")) {
                        Type = SegmentType.Raid10;
                    } else if (value.equals("raid4")) {
                        Type = SegmentType.Raid4;
                    } else if (value.equals("raid5")) {
                        Type = SegmentType.Raid5;
                    } else if (value.equals("raid5_la")) {
                        Type = SegmentType.Raid5La;
                    } else if (value.equals("raid5_ra")) {
                        Type = SegmentType.Raid5Ra;
                    } else if (value.equals("raid5_ls")) {
                        Type = SegmentType.Raid5Ls;
                    } else if (value.equals("raid5_rs")) {
                        Type = SegmentType.Raid5Rs;
                    } else if (value.equals("raid6")) {
                        Type = SegmentType.Raid6;
                    } else if (value.equals("raid6_zr")) {
                        Type = SegmentType.Raid6Zr;
                    } else if (value.equals("raid6_nr")) {
                        Type = SegmentType.Raid6Nr;
                    } else if (value.equals("raid6_nc")) {
                        Type = SegmentType.Raid6Nc;
                    } else if (value.equals("thin-pool")) {
                        Type = SegmentType.ThinPool;
                    } else if (value.equals("thin")) {
                        Type = SegmentType.Thin;
                    }

                } else if (key.equals("stripe_count")) {
                    StripeCount = Metadata.parseNumericValue(parameter.getValue());
                } else if (key.equals("stripes")) {
                    if (parameter.getValue().trim().equals("[")) {
                        Stripes = parseStripesSection(data);
                    }

                } else {
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
        while ((line = Metadata.readLine(data)) != null) {
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
