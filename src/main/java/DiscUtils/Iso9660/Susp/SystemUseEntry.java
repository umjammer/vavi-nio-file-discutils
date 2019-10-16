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

package DiscUtils.Iso9660.Susp;

import java.nio.charset.Charset;

import DiscUtils.Streams.Util.EndianUtilities;


public abstract class SystemUseEntry {
    public String Name;

    public byte Version;

    public static SystemUseEntry parse(byte[] data,
                                       int offset,
                                       Charset encoding,
                                       SuspExtension extension,
                                       byte[] length) {
        if (data[offset] == 0) {
            // A zero-byte here is invalid and indicates an incorrectly written SUSP field.
            // Return null to indicate to the caller that SUSP parsing is terminated.
            length[0] = 0;
            return null;
        }

        String name = EndianUtilities.bytesToString(data, offset, 2);
        length[0] = data[offset + 2];
        byte version = data[offset + 3];
        String __dummyScrutVar0 = name;
        if (__dummyScrutVar0.equals("CE")) {
            return new ContinuationSystemUseEntry(name, length[0], version, data, offset);
        } else if (__dummyScrutVar0.equals("PD")) {
            return new PaddingSystemUseEntry(name, length[0], version);
        } else if (__dummyScrutVar0.equals("SP")) {
            return new SharingProtocolSystemUseEntry(name, length[0], version, data, offset);
        } else if (__dummyScrutVar0.equals("ST")) {
            return null;
        } else // Termination entry. There's no point in storing or validating this one.
        // Return null to indicate to the caller that SUSP parsing is terminated.
        if (__dummyScrutVar0.equals("ER")) {
            return new ExtensionSystemUseEntry(name, length[0], version, data, offset, encoding);
        } else if (__dummyScrutVar0.equals("ES")) {
            return new ExtensionSelectSystemUseEntry(name, length[0], version, data, offset);
        } else if (__dummyScrutVar0.equals("AA") || __dummyScrutVar0.equals("AB") || __dummyScrutVar0.equals("AS")) {
            return new GenericSystemUseEntry(name, length[0], version, data, offset);
        } else {
            // Placeholder support for Apple and Amiga extension records.
            if (extension == null) {
                return new GenericSystemUseEntry(name, length[0], version, data, offset);
            }

            return extension.parse(name, length[0], version, data, offset, encoding);
        }
    }

    protected void checkAndSetCommonProperties(String name, byte length, byte version, byte minLength, byte maxVersion) {
        if (length < minLength) {
            throw new IllegalArgumentException("Invalid SUSP " + Name + " entry - too short, only " + length + " bytes");
        }

        if (version > maxVersion || version == 0) {
            throw new UnsupportedOperationException("Unknown SUSP " + Name + " entry version: " + version);
        }

        Name = name;
        Version = version;
    }
}
