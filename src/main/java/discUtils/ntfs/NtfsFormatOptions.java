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

package discUtils.ntfs;

import dotnet4j.security.principal.SecurityIdentifier;


/**
 * Class representing NTFS formatting options.
 */
public final class NtfsFormatOptions {
    /**
     * Gets or sets the NTFS bootloader code to put in the formatted file system.
     */
    private byte[] bootCode;

    public byte[] getBootCode() {
        return bootCode;
    }

    public void setBootCode(byte[] value) {
        bootCode = value;
    }

    /**
     * Gets or sets the SID of the computer account that notionally formatted the
     * file system.
     *
     * Certain ACLs in the file system will refer to the 'local' administrator of
     * the indicated computer account.
     */
    private SecurityIdentifier computerAccount;

    public SecurityIdentifier getComputerAccount() {
        return computerAccount;
    }

    public void setComputerAccount(SecurityIdentifier value) {
        computerAccount = value;
    }
}
