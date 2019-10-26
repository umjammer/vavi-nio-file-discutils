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
//
// Symbolic names of BCD Elements taken from Geoff Chappell's website:
//  http://www.geoffchappell.com/viewer.htm?doc=notes/windows/boot/bcd/elements.htm
//
//

package DiscUtils.BootConfig;

import java.util.UUID;

import vavi.util.Debug;

import DiscUtils.BootConfig.ApplicationType;
import DiscUtils.BootConfig.BaseStorage;
import DiscUtils.BootConfig.BooleanElementValue;
import DiscUtils.BootConfig.DeviceElementValue;
import DiscUtils.BootConfig.ElementClass;
import DiscUtils.BootConfig.ElementFormat;
import DiscUtils.BootConfig.ElementValue;
import DiscUtils.BootConfig.GuidElementValue;
import DiscUtils.BootConfig.GuidListElementValue;
import DiscUtils.BootConfig.IntegerElementValue;
import DiscUtils.BootConfig.IntegerListElementValue;
import DiscUtils.BootConfig.StringElementValue;


/**
 * Represents an element in a Boot Configuration Database object.
 */
public class Element {
    private ApplicationType _appType = ApplicationType.None;

    private final int _identifier;

    private final UUID _obj;

    private final BaseStorage _storage;

    private ElementValue _value;

    public Element(BaseStorage storage, UUID obj, ApplicationType appType, int identifier) {
        _storage = storage;
        _obj = obj;
        _appType = appType;
        _identifier = identifier;
    }

    /**
     * Gets the class of the element.
     */
    public ElementClass getClass_() {
        return ElementClass.valueOf((_identifier >>> 28) & 0xF);
    }

    /**
     * Gets the element's format.
     */
    public ElementFormat getFormat() {
        return ElementFormat.valueOf((_identifier >>> 24) & 0xF);
    }

    /**
     * Gets the friendly name of the element, if any.
     */
    public String getFriendlyName() {
        return "{" + identifierToName(_appType, _identifier) + "}";
    }

    /**
     * Gets or sets the element's value.
     */
    public ElementValue getValue() {
        if (_value == null) {
            _value = loadValue();
        }

        return _value;
    }

    public void setValue(ElementValue value) {
        if (getFormat() != value.getFormat()) {
            throw new IllegalArgumentException(String
                    .format("Attempt to assign %2$s value to %1$s format element", getFormat(), value.getFormat()));
        }

        _value = value;
        writeValue();
    }

    /**
     * Gets the element's id as a hex string.
     * 
     * @return A hex string.
     */
    public String toString() {
        return String.format("%8x", _identifier);
    }

    @SuppressWarnings("incomplete-switch")
    private static String identifierToName(ApplicationType appType, int identifier) {
        ElementClass idClass = getClass(identifier);
        if (idClass == ElementClass.Library) {
            switch (identifier) {
            case 0x11000001:
                return "device";
            case 0x12000002:
                return "path";
            case 0x12000004:
                return "description";
            case 0x12000005:
                return "locale";
            case 0x14000006:
                return "inherit";
            case 0x15000007:
                return "truncatememory";
            case 0x14000008:
                return "recoverysequence";
            case 0x16000009:
                return "recoveryenabled";
            case 0x1700000A:
                return "badmemorylist";
            case 0x1600000B:
                return "badmemoryaccess";
            case 0x1500000C:
                return "firstmegabytepolicy";
            case 0x16000010:
                return "bootdebug";
            case 0x15000011:
                return "debugtype";
            case 0x15000012:
                return "debugaddress";
            case 0x15000013:
                return "debugport";
            case 0x15000014:
                return "baudrate";
            case 0x15000015:
                return "channel";
            case 0x12000016:
                return "targetname";
            case 0x16000017:
                return "noumex";
            case 0x15000018:
                return "debugstart";
            case 0x16000020:
                return "bootems";
            case 0x15000022:
                return "emsport";
            case 0x15000023:
                return "emsbaudrate";
            case 0x12000030:
                return "loadoptions";
            case 0x16000040:
                return "advancedoptions";
            case 0x16000041:
                return "optionsedit";
            case 0x15000042:
                return "keyringaddress";
            case 0x16000046:
                return "graphicsmodedisabled";
            case 0x15000047:
                return "configaccesspolicy";
            case 0x16000048:
                return "nointegritychecks";
            case 0x16000049:
                return "testsigning";
            case 0x16000050:
                return "extendedinput";
            case 0x15000051:
                return "initialconsoleinput";
            }
        } else if (idClass == ElementClass.Application) {
            switch (appType) {
            case FirmwareBootManager:
            case BootManager:
                switch (identifier) {
                case 0x24000001:
                    return "displayorder";
                case 0x24000002:
                    return "bootsequence";
                case 0x23000003:
                    return "default";
                case 0x25000004:
                    return "timeout";
                case 0x26000005:
                    return "resume";
                case 0x23000006:
                    return "resumeobject";
                case 0x24000010:
                    return "toolsdisplayorder";
                case 0x26000020:
                    return "displaybootmenu";
                case 0x26000021:
                    return "noerrordisplay";
                case 0x21000022:
                    return "bcddevice";
                case 0x22000023:
                    return "bcdfilepath";
                case 0x27000030:
                    return "customactions";
                }
                break;
            case OsLoader:
                switch (identifier) {
                case 0x21000001:
                    return "osdevice";
                case 0x22000002:
                    return "systemroot";
                case 0x23000003:
                    return "resumeobject";
                case 0x26000010:
                    return "detecthal";
                case 0x22000011:
                    return "kernel";
                case 0x22000012:
                    return "hal";
                case 0x22000013:
                    return "dbgtransport";
                case 0x25000020:
                    return "nx";
                case 0x25000021:
                    return "pae";
                case 0x26000022:
                    return "winpe";
                case 0x26000024:
                    return "nocrashautoreboot";
                case 0x26000025:
                    return "lastknowngood";
                case 0x26000026:
                    return "oslnointegritychecks";
                case 0x26000027:
                    return "osltestsigning";
                case 0x26000030:
                    return "nolowmem";
                case 0x25000031:
                    return "removememory";
                case 0x25000032:
                    return "increaseuserva";
                case 0x25000033:
                    return "perfmem";
                case 0x26000040:
                    return "vga";
                case 0x26000041:
                    return "quietboot";
                case 0x26000042:
                    return "novesa";
                case 0x25000050:
                    return "clustermodeaddressing";
                case 0x26000051:
                    return "usephysicaldestination";
                case 0x25000052:
                    return "restrictapiccluster";
                case 0x26000060:
                    return "onecpu";
                case 0x25000061:
                    return "numproc";
                case 0x26000062:
                    return "maxproc";
                case 0x25000063:
                    return "configflags";
                case 0x26000070:
                    return "usefirmwarepcisettings";
                case 0x25000071:
                    return "msi";
                case 0x25000072:
                    return "pciexpress";
                case 0x25000080:
                    return "safeboot";
                case 0x26000081:
                    return "safebootalternateshell";
                case 0x26000090:
                    return "bootlog";
                case 0x26000091:
                    return "sos";
                case 0x260000A0:
                    return "debug";
                case 0x260000A1:
                    return "halbreakpoint";
                case 0x260000B0:
                    return "ems";
                case 0x250000C0:
                    return "forcefailure";
                case 0x250000C1:
                    return "driverloadfailurepolicy";
                case 0x250000E0:
                    return "bootstatuspolicy";
                }
                break;
            case Resume:
                switch (identifier) {
                case 0x21000001:
                    return "filedevice";
                case 0x22000002:
                    return "filepath";
                case 0x26000003:
                    return "customsettings";
                case 0x26000004:
                    return "pae";
                case 0x21000005:
                    return "associatedosdevice";
                case 0x26000006:
                    return "debugoptionenabled";
                }
                break;
            case MemoryDiagnostics:
                switch (identifier) {
                case 0x25000001:
                    return "passcount";
                case 0x25000002:
                    return "testmix";
                case 0x25000003:
                    return "failurecount";
                case 0x25000004:
                    return "testtofail";
                }
                break;
            case NtLoader:
            case SetupLoader:
                switch (identifier) {
                case 0x22000001:
                    return "bpbstring";
                }
                break;
            case Startup:
                switch (identifier) {
                case 0x26000001:
                    return "pxesoftreboot";
                case 0x22000002:
                    return "applicationname";
                }
                break;
            }
        } else if (idClass == ElementClass.Device) {
            switch (identifier) {
            case 0x35000001:
                return "ramdiskimageoffset";
            case 0x35000002:
                return "ramdisktftpclientport";
            case 0x31000003:
                return "ramdisksdidevice";
            case 0x32000004:
                return "ramdisksdipath";
            case 0x35000005:
                return "ramdiskimagelength";
            case 0x36000006:
                return "exportascd";
            case 0x35000007:
                return "ramdisktftpblocksize";
            }
        } else if (idClass == ElementClass.Hidden) {
            switch (identifier) {
            case 0x45000001:
                return "devicetype";
            case 0x42000002:
                return "apprelativepath";
            case 0x42000003:
                return "ramdiskdevicerelativepath";
            case 0x46000004:
                return "omitosloaderelements";
            case 0x46000010:
                return "recoveryos";
            }
        }

        return String.format("%8x", identifier);
    }

    private static ElementClass getClass(int identifier) {
        return ElementClass.values()[((identifier >>> 28) & 0xF)];
    }

    private ElementValue loadValue() {
Debug.println("getFormat():" + getFormat());
        switch (getFormat()) {
        case Boolean:
            return new BooleanElementValue(_storage.getBinary(_obj, _identifier));
        case Device:
            return new DeviceElementValue(_storage.getBinary(_obj, _identifier));
        case Guid:
            return new GuidElementValue(_storage.getString(_obj, _identifier));
        case GuidList:
            return new GuidListElementValue(_storage.getMultiString(_obj, _identifier));
        case Integer:
            return new IntegerElementValue(_storage.getBinary(_obj, _identifier));
        case IntegerList:
            return new IntegerListElementValue(_storage.getBinary(_obj, _identifier));
        case String:
            return new StringElementValue(_storage.getString(_obj, _identifier));
        default:
            throw new IllegalArgumentException("Unknown element format: " + getFormat());
        }
    }

    private void writeValue() {
        switch (_value.getFormat()) {
        case Boolean:
            _storage.setBinary(_obj, _identifier, ((BooleanElementValue) _value).getBytes());
            break;
        case Device:
            _storage.setBinary(_obj, _identifier, ((DeviceElementValue) _value).getBytes());
            break;
        case GuidList:
            _storage.setMultiString(_obj, _identifier, ((GuidListElementValue) _value).getGuidStrings());
            break;
        case Integer:
            _storage.setBinary(_obj, _identifier, ((IntegerElementValue) _value).getBytes());
            break;
        case IntegerList:
            _storage.setBinary(_obj, _identifier, ((IntegerListElementValue) _value).getBytes());
            break;
        case Guid:
        case String:
            _storage.setString(_obj, _identifier, _value.toString());
            break;
        default:
            throw new IllegalArgumentException("Unknown element format: " + getFormat());
        }
    }
}
