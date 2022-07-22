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

package discUtils.iscsi;

import java.util.List;


/**
 * Information about an iSCSI Target.
 *
 * A target contains zero or more LUNs.
 */
public class TargetInfo {

    private List<TargetAddress> addresses;

    /**
     * Initializes a new instance of the TargetInfo class.
     *
     * @param name The name of the Target.
     * @param addresses The network addresses of the Target.
     */
    public TargetInfo(String name, List<TargetAddress> addresses) {
        this.name = name;
        this.addresses = addresses;
    }

    /**
     * Gets the network addresses of the Target.
     */
    public List<TargetAddress> getAddresses() {
        return addresses;
    }

    /**
     * Gets the name of the Target.
     */
    private String name;

    public String getName() {
        return name;
    }

    /**
     * Gets the primary address of the Target as a string.
     *
     * @return String of the form host[:port][,group]/name.
     */
    public String toString() {
        return addresses.get(0) + "/" + name;
    }
}
