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
// Symbolic names of BCD Objects taken from Geoff Chappell's website:
//  http://www.geoffchappell.com/viewer.htm?doc=notes/windows/boot/bcd/objects.htm
//
//

package DiscUtils.BootConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vavi.util.Debug;


/**
 * Represents a Boot Configuration Database object (application, device or
 * inherited settings).
 */
public class BcdObject {
    /**
     * Well-known object for Emergency Management Services settings.
     */
    public static final String EmsSettingsGroupId = "{0CE4991B-E6B3-4B16-B23C-5E0D9250E5D9}";

    /**
     * Well-known object for the Resume boot loader.
     */
    public static final String ResumeLoaderSettingsGroupId = "{1AFA9C49-16AB-4A5C-4A90-212802DA9460}";

    /**
     * Alias for the Default boot entry.
     */
    public static final String DefaultBootEntryId = "{1CAE1EB7-A0DF-4D4D-9851-4860E34EF535}";

    /**
     * Well-known object for Emergency Management Services settings.
     */
    public static final String DebuggerSettingsGroupId = "{4636856E-540F-4170-A130-A84776F4C654}";

    /**
     * Well-known object for NTLDR application.
     */
    public static final String WindowsLegacyNtldrId = "{466F5A88-0AF2-4F76-9038-095B170DC21C}";

    /**
     * Well-known object for bad memory settings.
     */
    public static final String BadMemoryGroupId = "{5189B25C-5558-4BF2-BCA4-289B11BD29E2}";

    /**
     * Well-known object for Boot Loader settings.
     */
    public static final String BootLoaderSettingsGroupId = "{6EFB52BF-1766-41DB-A6B3-0EE5EFF72BD7}";

    /**
     * Well-known object for EFI setup.
     */
    public static final String WindowsSetupEfiId = "{7254A080-1510-4E85-AC0F-E7FB3D444736}";

    /**
     * Well-known object for Global settings.
     */
    public static final String GlobalSettingsGroupId = "{7EA2E1AC-2E61-4728-AAA3-896D9D0A9F0E}";

    /**
     * Well-known object for Windows Boot Manager.
     */
    public static final String WindowsBootManagerId = "{9DEA862C-5CDD-4E70-ACC1-F32B344D4795}";

    /**
     * Well-known object for PCAT Template.
     */
    public static final String WindowsOsTargetTemplatePcatId = "{A1943BBC-EA85-487C-97C7-C9EDE908A38A}";

    /**
     * Well-known object for Firmware Boot Manager.
     */
    public static final String FirmwareBootManagerId = "{A5A30FA2-3D06-4E9F-B5F4-A01DF9D1FCBA}";

    /**
     * Well-known object for Windows Setup RAMDISK options.
     */
    public static final String WindowsSetupRamdiskOptionsId = "{AE5534E0-A924-466C-B836-758539A3EE3A}";

    /**
     * Well-known object for EFI template.
     */
    public static final String WindowsOsTargetTemplateEfiId = "{B012B84D-C47C-4ED5-B722-C0C42163E569}";

    /**
     * Well-known object for Windows memory tester application.
     */
    public static final String WindowsMemoryTesterId = "{B2721D73-1DB4-4C62-BF78-C548A880142D}";

    /**
     * Well-known object for Windows PCAT setup.
     */
    public static final String WindowsSetupPcatId = "{CBD971BF-B7B8-4885-951A-FA03044F5D71}";

    /**
     * Alias for the current boot entry.
     */
    public static final String CurrentBootEntryId = "{FA926493-6F1C-4193-A414-58F0B2456D1E}";

    private static final Map<String, UUID> _nameToGuid;

    private static final Map<UUID, String> _guidToName;

    private UUID _id;

    private final BaseStorage _storage;

    private final int _type;

    static {
        _nameToGuid = new HashMap<>();
        _guidToName = new HashMap<>();
        addMapping("{emssettings}", EmsSettingsGroupId);
        addMapping("{resumeloadersettings}", ResumeLoaderSettingsGroupId);
        addMapping("{default}", DefaultBootEntryId);
        addMapping("{dbgsettings}", DebuggerSettingsGroupId);
        addMapping("{legacy}", WindowsLegacyNtldrId);
        addMapping("{ntldr}", WindowsLegacyNtldrId);
        addMapping("{badmemory}", BadMemoryGroupId);
        addMapping("{bootloadersettings}", BootLoaderSettingsGroupId);
        addMapping("{globalsettings}", GlobalSettingsGroupId);
        addMapping("{bootmgr}", WindowsBootManagerId);
        addMapping("{fwbootmgr}", FirmwareBootManagerId);
        addMapping("{ramdiskoptions}", WindowsSetupRamdiskOptionsId);
        addMapping("{memdiag}", WindowsMemoryTesterId);
        addMapping("{current}", CurrentBootEntryId);
    }

    public BcdObject(BaseStorage store, UUID id) {
        _storage = store;
        _id = id;
        _type = _storage.getObjectType(id);
    }

    /**
     * Gets the image type for this application.
     */
    public ApplicationImageType getApplicationImageType() {
        return isApplication() ? ApplicationImageType.values()[((_type & 0x00F00000) >>> 20)] : ApplicationImageType.None;
    }

    /**
     * Gets the application type for this application.
     */
    public ApplicationType getApplicationType() {
        return isApplication() ? ApplicationType.values()[(_type & 0xFFFFF)] : ApplicationType.None;
    }

    /**
     * Gets the elements in this object.
     */
    public List<Element> getElements() {
        List<Element> result = new ArrayList<>();
        for (int el : _storage.enumerateElements(_id)) {
            result.add(new Element(_storage, _id, getApplicationType(), el));
        }
        return result;
    }

    /**
     * Gets the friendly name for this object, if known.
     */
    public String getFriendlyName() {
        if (_guidToName.containsKey(_id)) {
            return _guidToName.get(_id);
        }

        return String.format("{%s}", _id);
    }

    /**
     * Gets the identity of this object.
     */
    public UUID getIdentity() {
        return _id;
    }

    private boolean isApplication() {
        return getObjectType() == ObjectType.Application;
    }

    /**
     * Gets the object type for this object.
     */
    public ObjectType getObjectType() {
        return ObjectType.values()[((_type >>> 28) & 0xF)];
    }

    /**
     * Indicates if the settings in this object are inheritable by another object.
     *
     * @param type The type of the object to test for inheritability.
     * @return {@code true} if the settings can be inherited, else {@code false} .
     */
    public boolean isInheritableBy(ObjectType type) {
        if (type == ObjectType.Inherit) {
            throw new IllegalArgumentException("Can not test inheritability by inherit objects");
        }

        if (getObjectType() != ObjectType.Inherit) {
            return false;
        }

        InheritType setting = InheritType.values()[((_type & 0x00F00000) >>> 20)];
        return setting == InheritType.AnyObject ||
            (setting == InheritType.ApplicationObjects && type == ObjectType.Application) ||
            (setting == InheritType.DeviceObjects && type == ObjectType.Device);
    }

    /**
     * Indicates if this object has a specific element.
     *
     * @param id The identity of the element to look for.
     * @return {@code true} if present, else {@code false} .
     */
    public boolean hasElement(int id) {
        return _storage.hasValue(_id, id);
    }

    /**
     * Indicates if this object has a specific element.
     *
     * @param id The identity of the element to look for.
     * @return {@code true} if present, else {@code false} .
     */
    public boolean hasElement(WellKnownElement id) {
        return hasElement(id.getValue());
    }

    /**
     * Gets a specific element in this object.
     *
     * @param id The identity of the element to look for.
     * @return The element object.
     */
    public Element getElement(int id) {
Debug.println(id + ", " + hasElement(id));
        if (hasElement(id)) {
            return new Element(_storage, _id, getApplicationType(), id);
        }

        return null;
    }

    /**
     * Gets a specific element in this object.
     *
     * @param id The identity of the element to look for.
     * @return The element object.
     */
    public Element getElement(WellKnownElement id) {
        return getElement(id.getValue());
    }

    /**
     * Adds an element in this object.
     *
     * @param id The identity of the element to add.
     * @param initialValue The initial value of the element.
     * @return The element object.
     */
    public Element addElement(int id, ElementValue initialValue) {
        _storage.createElement(_id, id);
        Element el = new Element(_storage, _id, getApplicationType(), id);
        el.setValue(initialValue);
        return el;
    }

    /**
     * Adds an element in this object.
     *
     * @param id The identity of the element to add.
     * @param initialValue The initial value of the element.
     * @return The element object.
     */
    public Element addElement(WellKnownElement id, ElementValue initialValue) {
        return addElement(id.getValue(), initialValue);
    }

    /**
     * Removes a specific element.
     *
     * @param id The element to remove.
     */
    public void removeElement(int id) {
        _storage.deleteElement(_id, id);
    }

    /**
     * Removes a specific element.
     *
     * @param id The element to remove.
     */
    public void removeElement(WellKnownElement id) {
        removeElement(id.getValue());
    }

    /**
     * Returns the object identity as a GUID string.
     *
     * @return A string representation, with surrounding curly braces.
     */
    public String toString() {
        return String.format("{%s}", _id);
    }

    public static int makeApplicationType(ApplicationImageType imageType, ApplicationType appType) {
        return 0x10000000 | ((imageType.ordinal() << 20) & 0x00F00000) | (appType.ordinal() & 0x0000FFFF);
    }

    public static int makeInheritType(InheritType inheritType) {
        return 0x20000000 | ((inheritType.ordinal() << 20) & 0x00F00000);
    }

    private static void addMapping(String name, String id) {
        UUID guid = UUID.fromString(id.replaceAll("[\\{\\}]", ""));
        _nameToGuid.put(name, guid);
        _guidToName.put(guid, name);
    }
}
