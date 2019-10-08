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

package DiscUtils.Core.Partitions;

import java.util.UUID;


/**
 * Convenient access to well known GPT partition types.
 */
public class GuidPartitionTypes {
    /**
     * EFI system partition.
     */
    public static final UUID EfiSystem = UUID.fromString("C12A7328-F81F-11D2-BA4B-00A0C93EC93B");

    /**
     * BIOS boot partition.
     */
    public static final UUID BiosBoot = UUID.fromString("21686148-6449-6E6F-744E-656564454649");

    /**
     * Microsoft reserved partition.
     */
    public static final UUID MicrosoftReserved = UUID.fromString("E3C9E316-0B5C-4DB8-817D-F92DF00215AE");

    /**
     * Windows basic data partition.
     */
    public static final UUID WindowsBasicData = UUID.fromString("EBD0A0A2-B9E5-4433-87C0-68B6B72699C7");

    /**
     * Linux LVM partition.
     */
    public static final UUID LinuxLvm = UUID.fromString("E6D6D379-F507-44C2-A23C-238F2A3DF928");

    /**
     * Linux swap partition.
     */
    public static final UUID LinuxSwap = UUID.fromString("0657FD6D-A4AB-43C4-84E5-0933C84B4F4F");

    /**
     * Windows Logical Disk Manager metadata.
     */
    public static final UUID WindowsLdmMetadata = UUID.fromString("5808C8AA-7E8F-42E0-85D2-E1E90434CFB3");

    /**
     * Windows Logical Disk Manager data.
     */
    public static final UUID WindowsLdmData = UUID.fromString("AF9B60A0-1431-4F62-BC68-3311714A69AD");

    /**
     * Converts a well known partition type to a UUID.
     *
     * @param wellKnown The value to convert.
     * @return The GUID value.
     */
    public static UUID convert(WellKnownPartitionType wellKnown) {
        switch (wellKnown) {
        case Linux:
        case WindowsFat:
        case WindowsNtfs:
            return WindowsBasicData;
        case LinuxLvm:
            return LinuxLvm;
        case LinuxSwap:
            return LinuxSwap;
        default:
            throw new IllegalArgumentException("Unknown partition type");

        }
    }

}
