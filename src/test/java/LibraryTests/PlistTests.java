//
// Copyright (c) 2017, Quamotion bvba
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

package LibraryTests;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import DiscUtils.Core.Plist;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.JavaIOStream;


public class PlistTests {

    @Test
    public void parse() throws Exception {
        try (Stream stream = new JavaIOStream(getClass().getResourceAsStream("/plist.xml"), null)) {
            Map<String, Object> plist = Plist.parse(stream);
            assertNotNull(plist);
            assertEquals(4, plist.size()); // plist.keySet().size()
            assertEquals("valueA", plist.get("keyA"));
            assertEquals("value&B", plist.get("key&B"));
        }
    }
}
