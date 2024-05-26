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

package discUtils.registry;

import java.nio.charset.StandardCharsets;

import vavi.util.ByteUtil;


/**
 * A registry value.
 */
public final class RegistryValue {

    private final ValueCell cell;

    private final RegistryHive hive;

    public RegistryValue(RegistryHive hive, ValueCell cell) {
        this.hive = hive;
        this.cell = cell;
    }

    /**
     * Gets the type of the value.
     */
    public RegistryValueType getDataType() {
        return cell.getDataType();
    }

    /**
     * Gets the name of the value, or empty string if unnamed.
     */
    public String getName() {
        return cell.getName() != null ? cell.getName() : "";
    }

    /**
     * Gets the value data mapped to a .net object.
     * <p>
     * The mapping from registry type of .NET type is as follows:
     * <pre>
     * Value Type: .NET type
     *
     * String:         string
     * ExpandString:   string
     * Link:           string
     * DWord:          uint
     * DWordBigEndian: uint
     * MultiString:    string[]
     * QWord:          ulong
     * </pre>
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
        if (cell.getDataLength() < 0) {
            int len = cell.getDataLength() & 0x7FFFFFFF;
            byte[] buffer = new byte[4];
            ByteUtil.writeLeInt(cell.getDataIndex(), buffer, 0);
            byte[] result = new byte[len];
            System.arraycopy(buffer, 0, result, 0, len);
            return result;
        }

        return hive.rawCellData(cell.getDataIndex(), cell.getDataLength());
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
            if (cell.getDataLength() >= 0) {
                hive.freeCell(cell.getDataIndex());
            }

            cell.setDataLength(count | 0x80000000);
            cell.setDataIndex(ByteUtil.readLeInt(data, offset));
            cell.setDataType(valueType);
        } else {
            if (cell.getDataIndex() == -1 || cell.getDataLength() < 0) {
                cell.setDataIndex(hive.allocateRawCell(count));
            }

            if (!hive.writeRawCellData(cell.getDataIndex(), data, offset, count)) {
                int newDataIndex = hive.allocateRawCell(count);
                hive.writeRawCellData(newDataIndex, data, offset, count);
                hive.freeCell(cell.getDataIndex());
                cell.setDataIndex(newDataIndex);
            }

            cell.setDataLength(count);
            cell.setDataType(valueType);
        }
        hive.updateCell(cell, false);
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
        return getName() + ":" + getDataType() + ":" + dataAsString();
    }

    private static Object convertToObject(byte[] data, RegistryValueType type) {
        return switch (type) {
            case String, ExpandString, Link ->
                    new String(data, StandardCharsets.UTF_16LE).replaceAll("(^\0*|\0*$)", "");
            case Dword -> ByteUtil.readLeInt(data, 0);
            case DwordBigEndian -> ByteUtil.readBeInt(data, 0);
            case MultiString -> {
                String multiString = new String(data, StandardCharsets.UTF_16LE).replaceAll("(^\0*|\0*$)", "");
                yield multiString.split("\0");
            }
            case QWord -> "" + ByteUtil.readLeLong(data, 0);
            default -> data;
        };
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
            data = strValue.getBytes(StandardCharsets.UTF_16LE);
            break;
        case Dword:
            data = new byte[4];
            ByteUtil.writeLeInt((int) value, data, 0);
            break;
        case DwordBigEndian:
            data = new byte[4];
            ByteUtil.writeBeInt((int) value, data, 0);
            break;
        case MultiString:
            String multiStrValue = String.join("\0", (String[]) value) + "\0";
            data = multiStrValue.getBytes(StandardCharsets.UTF_16LE);
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
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < Math.min(data.length, 8); ++i) {
                result.append(String.format("%2x ", (int) data[i]));
            }
            return result + String.format(" (%d bytes)", data.length);
        }
    }
}
