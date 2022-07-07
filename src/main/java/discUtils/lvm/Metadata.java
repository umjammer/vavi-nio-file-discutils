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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import dotnet4j.Tuple;


public class Metadata {
    public long CreationTime;

    public String CreationHost;

    public String Description;

    public String Contents;

    public int Version;

    public List<MetadataVolumeGroupSection> VolumeGroupSections;

    private static final ZonedDateTime DOTNET_MAX = Instant.parse("9999-12-31T23:59:59.999999900Z").atZone(ZoneId.of("UTC"));

    private static final long _maxSeconds = Duration.between(Instant.EPOCH, DOTNET_MAX).getSeconds();

    public static Metadata parse(String metadata) {
        try (Scanner reader = new Scanner(metadata)) {
            Metadata result = new Metadata();
            result.parse(reader);
            return result;
        }
    }

    private void parse(Scanner data) {
        String line;
        List<MetadataVolumeGroupSection> vgSection = new ArrayList<>();
        while (data.hasNextLine()) {
            line = readLine(data);
            if (line.equals(""))
                continue;

            if (line.contains("=")) {
                Tuple<String, String> parameter = parseParameter(line);
                String paramValue = parameter.getKey().trim().toLowerCase();
                switch (paramValue) {
                case "contents":
                    Contents = parseStringValue(parameter.getValue());
                    break;
                case "version":
                    Version = (int) parseNumericValue(parameter.getValue());
                    break;
                case "description":
                    Description = parseStringValue(parameter.getValue());
                    break;
                case "creation_host":
                    CreationHost = parseStringValue(parameter.getValue());
                    break;
                case "creation_time":
                    CreationTime = parseDateTimeValue(parameter.getValue());
                    break;
                default:
                    throw new IndexOutOfBoundsException("Unexpected parameter in global metadata: " + parameter.getKey());
                }
            } else if (line.endsWith("{")) {
                MetadataVolumeGroupSection vg = new MetadataVolumeGroupSection();
                vg.parse(line, data);
                vgSection.add(vg);
            }
        }
        VolumeGroupSections = vgSection;
    }

    public static String readLine(Scanner data) {
        String line = data.nextLine();
        return removeComment(line).trim();
    }

    public static String[] parseArrayValue(String value) {
        String[] values = Arrays.stream(value.replaceAll("[\\[\\]]", "").split(","))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        for (int i = 0; i < values.length; i++) {
            values[i] = Metadata.parseStringValue(values[i]);
        }
        return values;
    }

    public static String parseStringValue(String value) {
        return value.trim().replaceAll("\"", "");
    }

    public static long parseDateTimeValue(String value) {
        long numeric = parseNumericValue(value);
        if (numeric > _maxSeconds)
            return Long.MAX_VALUE;

        return Instant.EPOCH.plusSeconds(numeric).toEpochMilli();
    }

    public static long parseNumericValue(String value) {
        return Integer.parseInt(value.trim());
    }

    public static Tuple<String, String> parseParameter(String line) {
        int index = line.indexOf("=");
        if (index < 0)
            throw new IllegalArgumentException("invalid parameter line: " + line);
        return new Tuple<>(line.substring(0, index).trim(), line.substring(index + 1).trim());
    }

    public static String removeComment(String line) {
        int index = line.indexOf("#");
        if (index < 0)
            return line;
        return line.substring(0, index);
    }
}
