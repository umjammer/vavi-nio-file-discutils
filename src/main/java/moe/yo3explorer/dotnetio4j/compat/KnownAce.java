//
// Copyright (C) 2006-2007 Novell, Inc (http://www.novell.com)
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//

package moe.yo3explorer.dotnetio4j.compat;

import java.util.EnumSet;


/**
 * System.Security.AccessControl.KnownAce implementation
 *
 * @author Dick Porter <dick@ximian.com>
 * @author Atsushi Enomoto <atsushi@ximian.com>
 * @author Kenneth Bell
 */
public abstract class KnownAce extends GenericAce {
    protected int accessMask;

    protected SecurityIdentifier securityIdentifier;

    KnownAce(AceType type, EnumSet<AceFlags> flags) {
        super(type, flags);
    }

    KnownAce(byte[] binaryForm, int offset) {
        super(binaryForm, offset);
    }

    public int getAccessMask() {
        return accessMask;
    }

    void setAccessMask(int value) {
        accessMask = value;
    }

    public SecurityIdentifier getSecurityIdentifier() {
        return securityIdentifier;
    }

    void setSecurityIdentifier(SecurityIdentifier value) {
        securityIdentifier = value;
    }

    static String getSddlAccessRights(int accessMask) {
        String ret = getSddlAliasRights(accessMask);
        if (ret != null && !ret.isEmpty())
            return ret;

        return String.format("0x%x", accessMask);
    }

    private static String getSddlAliasRights(int accessMask) {
        SddlAccessRight[] rights = SddlAccessRight.decompose(accessMask);
        if (rights == null)
            return null;

        StringBuilder ret = new StringBuilder();
        for (SddlAccessRight right : rights) {
            ret.append(right.Name);
        }

        return ret.toString();
    }
}
