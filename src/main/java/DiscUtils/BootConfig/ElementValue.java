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

package DiscUtils.BootConfig;

import java.util.UUID;

import DiscUtils.Core.PhysicalVolumeInfo;


/**
 * The value of an element.
 */
public abstract class ElementValue {
    /**
     * Gets the format of the value.
     */
    public abstract ElementFormat getFormat();

    /**
     * Gets the parent object (only for Device values).
     */
    public UUID getParentObject() {
        return new UUID(0, 0);
    }

    /**
     * Gets a value representing a device (aka partition).
     * 
     * @param parentObject Object containing detailed information about the
     *            device.
     * @param physicalVolume The volume to represent.
     * @return The value as an object.
     */
    public static ElementValue forDevice(UUID parentObject, PhysicalVolumeInfo physicalVolume) {
        return new DeviceElementValue(parentObject, physicalVolume);
    }

    /**
     * Gets a value representing the logical boot device.
     * 
     * @return The boot pseudo-device as an object.
     */
    public static ElementValue forBootDevice() {
        return new DeviceElementValue();
    }

    /**
     * Gets a value representing a string value.
     * 
     * @param value The value to convert.
     * @return The value as an object.
     */
    public static ElementValue forString(String value) {
        return new StringElementValue(value);
    }

    /**
     * Gets a value representing an integer value.
     * 
     * @param value The value to convert.
     * @return The value as an object.
     */
    public static ElementValue forInteger(long value) {
        return new IntegerElementValue(value);
    }

    /**
     * Gets a value representing an integer list value.
     * 
     * @param values The value to convert.
     * @return The value as an object.
     */
    public static ElementValue forIntegerList(long[] values) {
        long[] ulValues = new long[values.length];
        System.arraycopy(values, 0, ulValues, 0, values.length);
        return new IntegerListElementValue(ulValues);
    }

    /**
     * Gets a value representing a boolean value.
     * 
     * @param value The value to convert.
     * @return The value as an object.
     */
    public static ElementValue forBoolean(boolean value) {
        return new BooleanElementValue(value);
    }

    /**
     * Gets a value representing a GUID value.
     * 
     * @param value The value to convert.
     * @return The value as an object.
     */
    public static ElementValue forGuid(UUID value) {
        return new GuidElementValue(String.format("{%s}", value));
    }

    /**
     * Gets a value representing a GUID list value.
     * 
     * @param values The value to convert.
     * @return The value as an object.
     */
    public static ElementValue forGuidList(UUID[] values) {
        String[] strValues = new String[values.length];
        for (int i = 0; i < values.length; ++i) {
            strValues[i] = String.format("{%s}", values[i]);
        }
        return new GuidListElementValue(strValues);
    }
}
