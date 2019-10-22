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
 * System.Security.AccessControl.QualifiedAce implementation
 *
 * @author Dick Porter <dick@ximian.com>
 * @author Atsushi Enomoto <atsushi@ximian.com>
 * @author Kenneth Bell
 */
public abstract class QualifiedAce extends KnownAce {
    private byte[] opaque;

    QualifiedAce(AceType type, EnumSet<AceFlags> flags, byte[] opaque) {
        super(type, flags);

        setOpaque(opaque);
    }

    QualifiedAce(byte[] binaryForm, int offset) {
        super(binaryForm, offset);
    }

    public AceQualifier getAceQualifier() {
        switch (aceType) {
        case AccessAllowed:
        case AccessAllowedCallback:
        case AccessAllowedCallbackObject:
        case AccessAllowedCompound:
        case AccessAllowedObject:
            return AceQualifier.AccessAllowed;
        case AccessDenied:
        case AccessDeniedCallback:
        case AccessDeniedCallbackObject:
        case AccessDeniedObject:
            return AceQualifier.AccessDenied;
        case SystemAlarm:
        case SystemAlarmCallback:
        case SystemAlarmCallbackObject:
        case SystemAlarmObject:
            return AceQualifier.SystemAlarm;
        case SystemAudit:
        case SystemAuditCallback:
        case SystemAuditCallbackObject:
        case SystemAuditObject:
            return AceQualifier.SystemAudit;
        default:
            throw new IllegalArgumentException("Unrecognised ACE type: " + aceType);
        }
    }

    public boolean isCallback() {
        return aceType == AceType.AccessAllowedCallback || aceType == AceType.AccessAllowedCallbackObject
                || aceType == AceType.AccessDeniedCallback || aceType == AceType.AccessDeniedCallbackObject
                || aceType == AceType.SystemAlarmCallback || aceType == AceType.SystemAlarmCallbackObject
                || aceType == AceType.SystemAuditCallback || aceType == AceType.SystemAuditCallbackObject;
    }

    public int getOpaqueLength() {
        if (opaque == null)
            return 0;
        return opaque.length;
    }

    public byte[] getOpaque() {
        if (opaque == null)
            return null;
        return opaque.clone();
    }

    public void setOpaque(byte[] opaque) {
        if (opaque == null)
            this.opaque = null;
        else
            this.opaque = opaque.clone();
    }
}
