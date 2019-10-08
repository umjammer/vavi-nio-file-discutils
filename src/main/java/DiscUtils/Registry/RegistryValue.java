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

package DiscUtils.Registry;

import java.nio.charset.Charset;

import DiscUtils.Streams.Util.EndianUtilities;


/**
 * A registry value.
 */
public final class RegistryValue {
    private final ValueCell _cell;

    private final RegistryHive _hive;

    public RegistryValue(RegistryHive hive, ValueCell cell) {
        _hive = hive;
        _cell = cell;
    }

    /**
     * Gets the type of the value.
     */
    public RegistryValueType getDataType() {
        return _cell.getDataType();
    }

    /**
     * Gets the name of the value, or empty string if unnamed.
     */
    public String getName() {
        return _cell.getName() != null ? _cell.getName() : "";
    }

    /**
     * Gets the value data mapped to a .net object.
     * The mapping from registry type of .NET type is as follows:
     * Value Type.NET
     * typeStringstringExpandStringstringLinkstringDWorduintDWordBigEndianuintMultiStringstring[]QWordulong
     */
    public Object getValue() {
        return convertToObject(getData(), getDataType());
    }

    /**
     * The raw value data as a byte array.
     *
     * @return The value as a raw byte array.
     */
    public byte[] getData() {
        if (_cell.getDataLength() < 0) {
            int len = _cell.getDataLength() & 0x7FFFFFFF;
            byte[] buffer = new byte[4];
            EndianUtilities.writeBytesLittleEndian(_cell.getDataIndex(), buffer, 0);
            byte[] result = new byte[len];
            System.arraycopy(buffer, 0, result, 0, len);
            return result;
        }

        return _hive.rawCellData(_cell.getDataIndex(), _cell.getDataLength());
    }

    /**
     * Sets the value as raw bytes, with no validation that enough data is
     * specified for the given value type.
     *
     * @param data The data to store.
     * @param offset The offset within
     *            {@code data}
     *            of the first byte to store.
     * @param count The number of bytes to store.
     * @param valueType The type of the data.
     */
    public void setData(byte[] data, int offset, int count, RegistryValueType valueType) {
        // If we can place the data in the DataIndex field, do that to save space / allocation
        if ((valueType == RegistryValueType.Dword || valueType == RegistryValueType.DwordBigEndian) && count <= 4) {
            if (_cell.getDataLength() >= 0) {
                _hive.freeCell(_cell.getDataIndex());
            }

            _cell.setDataLength(count | 0x80000000);
            _cell.setDataIndex(EndianUtilities.toInt32LittleEndian(data, offset));
            _cell.setDataType(valueType);
        } else {
            if (_cell.getDataIndex() == -1 || _cell.getDataLength() < 0) {
                _cell.setDataIndex(_hive.allocateRawCell(count));
            }

            if (!_hive.writeRawCellData(_cell.getDataIndex(), data, offset, count)) {
                int newDataIndex = _hive.allocateRawCell(count);
                _hive.writeRawCellData(newDataIndex, data, offset, count);
                _hive.freeCell(_cell.getDataIndex());
                _cell.setDataIndex(newDataIndex);
            }

            _cell.setDataLength(count);
            _cell.setDataType(valueType);
        }
        _hive.updateCell(_cell, false);
    }

    /**
     * Sets the value stored.
     *
     * @param value The value to store.
     * @param valueType The registry type of the data.
     */
    public void setValue(Object value, RegistryValueType valueType) {
        if (valueType == RegistryValueType.None) {
            if (value instanceof Integer) {
                valueType = RegistryValueType.Dword;
            } else if (value instanceof byte[]) {
                valueType = RegistryValueType.Binary;
            } else if (value instanceof String[]) {
                valueType = RegistryValueType.MultiString;
            } else {
                valueType = RegistryValueType.String;
            }
        }

        byte[] data = convertToData(value, valueType);
        setData(data, 0, data.length, valueType);
    }

    /**
     * Gets a string representation of the registry value.
     *
     * @return The registry value as a string.
     */
    public String toString() {
        try {
            return getName() + ":" + getDataType() + ":" + dataAsString();
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    private static Object convertToObject(byte[] data, RegistryValueType type) {
        switch (type) {
        case String:
        case ExpandString:
        case Link:
            return new String(data, Charset.forName("Unicode")).replaceFirst("^\0*", "").replaceFirst("\0*$", "");
        case Dword:
            return EndianUtilities.toInt32LittleEndian(data, 0);
        case DwordBigEndian:
            return EndianUtilities.toInt32BigEndian(data, 0);
        case MultiString:
            String multiString = new String(data, Charset.forName("Unicode")).replaceFirst("^\0*", "").replaceFirst("\0*$", "");
            return multiString.split("\0");
        case QWord:
            return "" + EndianUtilities.toUInt64LittleEndian(data, 0);
        default:
            return data;
        }
    }

    private static byte[] convertToData(Object value, RegistryValueType valueType) {
        if (valueType == RegistryValueType.None) {
            throw new IllegalArgumentException("Specific registry value type must be specified");
        }

        byte[] data;
        switch (valueType) {
        case String:
        case ExpandString:
            String strValue = value.toString();
            data = strValue.getBytes(Charset.forName("Unicode"));
            break;
        case Dword:
            data = new byte[4];
            EndianUtilities.writeBytesLittleEndian((Integer) value, data, 0);
            break;
        case DwordBigEndian:
            data = new byte[4];
            EndianUtilities.writeBytesBigEndian((Integer) value, data, 0);
            break;
        case MultiString:
            String multiStrValue = String.join("\0", (String[]) value) + "\0";
            data = multiStrValue.getBytes(Charset.forName("Unicode"));
            break;
        default:
            data = (byte[]) value;
            break;
        }
        return data;
    }

    private String dataAsString() {
        switch (getDataType()) {
        case String:
        case ExpandString:
        case Link:
        case Dword:
        case DwordBigEndian:
        case QWord:
            return convertToObject(getData(), getDataType()).toString();
        case MultiString:
            return String.join(",", (String[]) convertToObject(getData(), getDataType()));
        default:
            byte[] data = getData();
            String result = "";
            for (int i = 0; i < Math.min(data.length, 8); ++i) {
                result += String.format("{0:X2} ", (int) data[i]);
            }
            return result + String.format(" ({0} bytes)", data.length);
        }
    }
}
