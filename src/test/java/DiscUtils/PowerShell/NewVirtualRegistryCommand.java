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

package DiscUtils.PowerShell;

import java.io.Closeable;

import DiscUtils.PowerShell.Conpat.PSCmdlet;
import DiscUtils.Registry.RegistryHive;
import moe.yo3explorer.dotnetio4j.Stream;


public class NewVirtualRegistryCommand extends PSCmdlet {
    private String __LiteralPath;

    public String getLiteralPath() {
        return __LiteralPath;
    }

    public void setLiteralPath(String value) {
        __LiteralPath = value;
    }

    protected void processRecord() {
        Stream hiveStream = Utilities.createPsPath(SessionState, getLiteralPath());
        try {
            hiveStream.setLength(0);
            Closeable __newVar0 = RegistryHive.create(hiveStream);
            try {
            } finally {
                if (__newVar0 != null)
                    __newVar0.close();
            }
        } finally {
            if (hiveStream != null)
                hiveStream.close();
        }
    }
}
