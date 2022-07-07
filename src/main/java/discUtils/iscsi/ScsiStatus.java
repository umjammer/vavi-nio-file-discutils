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

import java.util.Arrays;

/**
 * Enumeration of SCSI command status codes.
 */
public enum ScsiStatus {
    /**
     * Indicates that the command completed without error.
     */
    Good(0x00),
    /**
     * An unsupported condition occured.
     */
    CheckCondition(0x02),
    /**
     * For some commands only - indicates the specified condition was met.
     */
    ConditionMet(0x04),
    /**
     * The device is busy.
     */
    Busy(0x08),
    /**
     * Delivered command conflicts with an existing reservation.
     */
    ReservationConflict(0x18),
    /**
     * The buffer of outstanding commands is full.
     */
    TaskSetFull(0x28),
    /**
     * An ACA condition exists.
     */
    AcaActive(0x30),
    /**
     * The command was aborted.
     */
    TaskAborted(0x40);

    private final int value;

    public int getValue() {
        return value;
    }

    ScsiStatus(int value) {
        this.value = value;
    }

    public static ScsiStatus valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().get();
    }
}
