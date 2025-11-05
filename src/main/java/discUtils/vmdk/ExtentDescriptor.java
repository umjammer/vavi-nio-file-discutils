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

package discUtils.vmdk;

import java.util.ArrayList;
import java.util.List;


public class ExtentDescriptor {

    public ExtentDescriptor() {
    }

    public ExtentDescriptor(ExtentAccess access, long size, ExtentType type, String fileName, long offset) {
        setAccess(access);
        setSizeInSectors(size);
        setType(type);
        setFileName(fileName);
        setOffset(offset);
    }

    private ExtentAccess access = ExtentAccess.None;

    public ExtentAccess getAccess() {
        return access;
    }

    public void setAccess(ExtentAccess value) {
        access = value;
    }

    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String value) {
        fileName = value;
    }

    private long offset;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long value) {
        offset = value;
    }

    private long sizeInSectors;

    public long getSizeInSectors() {
        return sizeInSectors;
    }

    public void setSizeInSectors(long value) {
        sizeInSectors = value;
    }

    private ExtentType type = ExtentType.Flat;

    public ExtentType getType() {
        return type;
    }

    public void setType(ExtentType value) {
        type = value;
    }

    public static ExtentDescriptor parse(String descriptor) {
        List<String> elems = splitQuotedString(descriptor);
        if (elems.size() < 4) {
            throw new dotnet4j.io.IOException("Invalid extent descriptor: %s".formatted(descriptor));
        }

        ExtentDescriptor result = new ExtentDescriptor();
        result.setAccess(parseAccess(elems.get(0)));
        result.setSizeInSectors(Long.parseLong(elems.get(1)));
        result.setType(parseType(elems.get(2)));
        result.setFileName(elems.get(3).replaceFirst("^\"", "").replaceFirst("\"$", ""));
        if (elems.size() > 4) {
            result.setOffset(Long.parseLong(elems.get(4)));
        }

        return result;
    }

    public static ExtentAccess parseAccess(String access) {
        if ("NOACCESS".equals(access)) {
            return ExtentAccess.None;
        }

        if ("RDONLY".equals(access)) {
            return ExtentAccess.ReadOnly;
        }

        if ("RW".equals(access)) {
            return ExtentAccess.ReadWrite;
        }

        throw new IllegalArgumentException("Unknown access type");
    }

    public static String formatAccess(ExtentAccess access) {
        return switch (access) {
            case None -> "NOACCESS";
            case ReadOnly -> "RDONLY";
            case ReadWrite -> "RW";
        };
    }

    public static ExtentType parseType(String type) {
        if ("FLAT".equals(type)) {
            return ExtentType.Flat;
        }

        if ("SPARSE".equals(type)) {
            return ExtentType.Sparse;
        }

        if ("ZERO".equals(type)) {
            return ExtentType.Zero;
        }

        if ("VMFS".equals(type)) {
            return ExtentType.Vmfs;
        }

        if ("VMFSSPARSE".equals(type)) {
            return ExtentType.VmfsSparse;
        }

        if ("VMFSRDM".equals(type)) {
            return ExtentType.VmfsRdm;
        }

        if ("VMFSRAW".equals(type)) {
            return ExtentType.VmfsRaw;
        }

        throw new IllegalArgumentException("Unknown extent type");
    }

    public static String formatExtentType(ExtentType type) {
        return switch (type) {
            case Flat -> "FLAT";
            case Sparse -> "SPARSE";
            case Zero -> "ZERO";
            case Vmfs -> "VMFS";
            case VmfsSparse -> "VMFSSPARSE";
            case VmfsRdm -> "VMFSRDM";
            case VmfsRaw -> "VMFSRAW";
        };
    }

    public String toString() {
        try {
            String basic = formatAccess(access) + " " + sizeInSectors + " " + formatExtentType(type) + " \"" +
                           getFileName() + "\"";
            if (type != ExtentType.Sparse && type != ExtentType.VmfsSparse && type != ExtentType.Zero) {
                return basic + " " + offset;
            }

            return basic;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> splitQuotedString(String source) {
        List<String> result = new ArrayList<>();
        int idx = 0;
        while (idx < source.length()) {
            while (source.charAt(idx) == ' ' && idx < source.length()) {
                // Skip spaces
                idx++;
            }
            if (source.charAt(idx) == '"') {
                // A quoted value, find end of quotes...
                int start = idx;
                idx++;
                while (idx < source.length() && source.charAt(idx) != '"') {
                    idx++;
                }
                result.add(source.substring(start, idx + 1));
            } else {
                // An unquoted value, find end of value
                int start = idx;
                idx++;
                while (idx < source.length() && source.charAt(idx) != ' ') {
                    idx++;
                }
                result.add(source.substring(start, idx));
            }
            idx++;
        }
        return result;
    }
}
