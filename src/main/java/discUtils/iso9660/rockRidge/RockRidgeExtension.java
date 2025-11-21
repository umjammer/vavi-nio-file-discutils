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

package discUtils.iso9660.rockRidge;

import java.nio.charset.Charset;

import discUtils.iso9660.susp.GenericSystemUseEntry;
import discUtils.iso9660.susp.SuspExtension;
import discUtils.iso9660.susp.SystemUseEntry;


public final class RockRidgeExtension extends SuspExtension {

    public RockRidgeExtension(String identifier) {
        this.identifier = identifier;
    }

    private final String identifier;

    @Override public String getIdentifier() {
        return identifier;
    }

    @Override public SystemUseEntry parse(String name, byte length, byte version, byte[] data, int offset, Charset encoding) {
        return switch (name) {
            case "PX" -> new PosixFileInfoSystemUseEntry(name, length, version, data, offset);
            case "NM" -> new PosixNameSystemUseEntry(name, length, version, data, offset);
            case "CL" -> new ChildLinkSystemUseEntry(name, length, version, data, offset);
            case "TF" -> new FileTimeSystemUseEntry(name, length, version, data, offset);
            default -> new GenericSystemUseEntry(name, length, version, data, offset);
        };
    }
}
