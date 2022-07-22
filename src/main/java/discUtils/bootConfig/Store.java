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

package discUtils.bootConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import discUtils.registry.RegistryKey;


/**
 * Represents a Boot Configuration Database store (i.e. a BCD file).
 */
public class Store {

    private final BaseStorage store;

    /**
     * Initializes a new instance of the Store class.
     * 
     * @param key The registry key that is the root of the configuration
     *            database.
     */
    public Store(RegistryKey key) {
        store = new DiscUtilsRegistryStorage(key);
    }

    /**
     * Gets the objects within the store.
     */
    public List<BcdObject> getObjects() {
        List<BcdObject> result = new ArrayList<>();
        for (UUID obj : store.enumerateObjects()) {
            result.add(new BcdObject(store, obj));
        }
        return result;
    }

    /**
     * Initializes a new Boot Configuration Database.
     * 
     * @param root The registry key at the root of the database.
     * @return The BCD store.
     */
    public static Store initialize(RegistryKey root) {
        RegistryKey descKey = root.createSubKey("description");
        descKey.setValue("KeyName", "BCD00000001");
        root.createSubKey("Objects");
        return new Store(root);
    }

    /**
     * Gets an object from the store.
     * 
     * @param id The identity of the object.
     * @return The requested object, or {@code null}.
     */
    public BcdObject getObject(UUID id) {
        if (store.objectExists(id)) {
            return new BcdObject(store, id);
        }

        return null;
    }

    /**
     * Creates a specific object.
     * 
     * @param id The identity of the object to create.
     * @param type The object's type.
     * @return The object representing the new application.
     */
    public BcdObject createObject(UUID id, int type) {
        store.createObject(id, type);
        return new BcdObject(store, id);
    }

    /**
     * Creates an application object.
     * 
     * @param imageType The image type of the application.
     * @param appType The application's type.
     * @return The object representing the new application.
     */
    public BcdObject createApplication(ApplicationImageType imageType, ApplicationType appType) {
        UUID obj = store.createObject(UUID.randomUUID(), BcdObject.makeApplicationType(imageType, appType));
        return new BcdObject(store, obj);
    }

    /**
     * Creates an application object.
     * 
     * @param id The identity of the object to create.
     * @param imageType The image type of the application.
     * @param appType The application's type.
     * @return The object representing the new application.
     */
    public BcdObject createApplication(UUID id, ApplicationImageType imageType, ApplicationType appType) {
        UUID obj = store.createObject(id, BcdObject.makeApplicationType(imageType, appType));
        return new BcdObject(store, obj);
    }

    /**
     * Creates an 'inherit' object that contains common settings.
     * 
     * @param inheritType The type of object the settings apply to.
     * @return The object representing the new settings.
     */
    public BcdObject createInherit(InheritType inheritType) {
        UUID obj = store.createObject(UUID.randomUUID(), BcdObject.makeInheritType(inheritType));
        return new BcdObject(store, obj);
    }

    /**
     * Creates an 'inherit' object that contains common settings.
     * 
     * @param id The identity of the object to create.
     * @param inheritType The type of object the settings apply to.
     * @return The object representing the new settings.
     */
    public BcdObject createInherit(UUID id, InheritType inheritType) {
        UUID obj = store.createObject(id, BcdObject.makeInheritType(inheritType));
        return new BcdObject(store, obj);
    }

    /**
     * Creates a new device object, representing a partition.
     * 
     * @return The object representing the new device.
     */
    public BcdObject createDevice() {
        UUID obj = store.createObject(UUID.randomUUID(), 0x30000000);
        return new BcdObject(store, obj);
    }

    /**
     * Removes an object.
     * 
     * @param id The identity of the object to remove.
     */
    public void removeObject(UUID id) {
        store.deleteObject(id);
    }
}
