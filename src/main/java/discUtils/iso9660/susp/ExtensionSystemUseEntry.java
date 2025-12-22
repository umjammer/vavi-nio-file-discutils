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

import discUtils.iso9660.IsoUtilities;


public final class ExtensionSystemUseEntry extends SystemUseEntry {

    public String extensionDescriptor;

    public String extensionIdentifier;

    public String extensionSource;

    public final byte extensionVersion;

    public ExtensionSystemUseEntry(String name, byte length, byte version, byte[] data, int offset, Charset encoding) {
        checkAndSetCommonProperties(name, length, version, (byte) 8, (byte) 1);
        int lenId = data[offset + 4] & 0xff;
        int lenDescriptor = data[offset + 5] & 0xff;
        int lenSource = data[offset + 6] & 0xff;
        extensionVersion = data[offset + 7];
        if ((length & 0xff) < 8 + lenId + lenDescriptor + lenSource) {
            throw new IllegalArgumentException("Invalid SUSP ER entry - too short, only " + (length & 0xff) +
                " bytes - expected: " + (8 + lenId + lenDescriptor + lenSource));
        }

        extensionIdentifier = IsoUtilities.readChars(data, offset + 8, lenId, encoding);
        extensionDescriptor = IsoUtilities.readChars(data, offset + 8 + lenId, lenDescriptor, encoding);
        extensionSource = IsoUtilities.readChars(data, offset + 8 + lenId + lenDescriptor, lenSource, encoding);
    }
}
