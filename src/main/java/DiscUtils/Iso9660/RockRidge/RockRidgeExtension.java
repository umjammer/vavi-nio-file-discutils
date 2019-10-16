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

package DiscUtils.Iso9660.RockRidge;

import java.nio.charset.Charset;

import DiscUtils.Iso9660.Susp.GenericSystemUseEntry;
import DiscUtils.Iso9660.Susp.SuspExtension;
import DiscUtils.Iso9660.Susp.SystemUseEntry;


public final class RockRidgeExtension extends SuspExtension {
    public RockRidgeExtension(String identifier) {
        __Identifier = identifier;
    }

    private String __Identifier;

    public String getIdentifier() {
        return __Identifier;
    }

    public SystemUseEntry parse(String name, byte length, byte version, byte[] data, int offset, Charset encoding) {
        String __dummyScrutVar0 = name;
        if (__dummyScrutVar0.equals("PX")) {
            return new PosixFileInfoSystemUseEntry(name, length, version, data, offset);
        } else if (__dummyScrutVar0.equals("NM")) {
            return new PosixNameSystemUseEntry(name, length, version, data, offset);
        } else if (__dummyScrutVar0.equals("CL")) {
            return new ChildLinkSystemUseEntry(name, length, version, data, offset);
        } else if (__dummyScrutVar0.equals("TF")) {
            return new FileTimeSystemUseEntry(name, length, version, data, offset);
        } else {
            return new GenericSystemUseEntry(name, length, version, data, offset);
        }
    }
}
