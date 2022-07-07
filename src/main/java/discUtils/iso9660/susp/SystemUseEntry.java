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

package discUtils.iso9660.susp;

import java.nio.charset.Charset;

import discUtils.streams.util.EndianUtilities;


public abstract class SystemUseEntry {
    public String _name;

    public byte _version;

    /**
     * @param length {@cs out}
     */
    public static SystemUseEntry parse(byte[] data, int offset, Charset encoding, SuspExtension extension, byte[] length) {
        if (data[offset] == 0) {
            // A zero-byte here is invalid and indicates an incorrectly written SUSP field.
            // Return null to indicate to the caller that SUSP parsing is terminated.
            length[0] = 0;

            return null;
        }

        String name = EndianUtilities.bytesToString(data, offset, 2);
        length[0] = data[offset + 2];
        byte version = data[offset + 3];

        switch (name) {
        case "CE":
            return new ContinuationSystemUseEntry(name, length[0], version, data, offset);
        case "PD":
            return new PaddingSystemUseEntry(name, length[0], version);
        case "SP":
            return new SharingProtocolSystemUseEntry(name, length[0], version, data, offset);
        case "ST":
            // Termination entry. There's no point in storing or validating this one.
            // Return null to indicate to the caller that SUSP parsing is terminated.
            return null;
        case "ER":
            return new ExtensionSystemUseEntry(name, length[0], version, data, offset, encoding);
        case "ES":
            return new ExtensionSelectSystemUseEntry(name, length[0], version, data, offset);
        case "AA":
        case "AB":
        case "AS":
            // Placeholder support for Apple and Amiga extension records.
            return new GenericSystemUseEntry(name, length[0], version, data, offset);
        default:
            if (extension == null) {
                return new GenericSystemUseEntry(name, length[0], version, data, offset);
            }

            return extension.parse(name, length[0], version, data, offset, encoding);
        }
    }

    protected void checkAndSetCommonProperties(String name, byte length, byte version, byte minLength, byte maxVersion) {
        if ((length & 0xff) < (minLength & 0xff)) {
            throw new IllegalArgumentException("Invalid SUSP " + _name + " entry - too short, only " + length + " bytes");
        }

        if ((version & 0xff) > (maxVersion & 0xff) || version == 0) {
            throw new UnsupportedOperationException("Unknown SUSP " + _name + " entry version: " + version);
        }

        _name = name;
        _version = version;
    }
}
