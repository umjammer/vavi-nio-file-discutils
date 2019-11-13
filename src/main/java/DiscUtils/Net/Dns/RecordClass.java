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

package DiscUtils.Net.Dns;

import java.util.Arrays;

/**
 * Enumeration of known DNS record classes (CLASS in DNS).
 */
public enum RecordClass {
    /**
     * No class defined.
     */
    None(0),
    /**
     * The Internet class.
     */
    Internet(1),
    /**
     * The CSNET class.
     */
    CSNet(2),
    /**
     * The CHAOS network class.
     */
    Chaos(3),
    /**
     * The Hesiod class.
     */
    Hesiod(4),
   /**
     * Wildcard that matches any class.
     */
    Any(255);

    private int value;

    public int getValue() {
        return value;
    }

    private RecordClass(int value) {
        this.value = value;
    }

    public static RecordClass valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().get();
    }
}
