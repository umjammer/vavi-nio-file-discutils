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

package discUtils.dmg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ResourceFork {
    private final List<Resource> _resources;

    public ResourceFork(List<Resource> resources) {
        _resources = resources;
    }

    public List<Resource> getAllResources(String type) {
        List<Resource> results = new ArrayList<>();
        for (Resource res : _resources) {
            if (res.getType().equals(type)) {
                results.add(res);
            }

        }
        return results;
    }

    public static ResourceFork fromPlist(Map<String, Object> plist) {
        if (!plist.containsKey("resource-fork")) {
            throw new IllegalArgumentException("plist doesn't contain resource fork");
        }
        Object typesObject = plist.get("resource-fork");

        Map<String, Object> types = (Map) typesObject;
        List<Resource> resources = new ArrayList<>();
        for (String type : types.keySet()) {
            List<Object> typeResources = (List) types.get(type);
            for (Object typeResource : typeResources) {
                resources.add(Resource.fromPlist(type, (Map) typeResource));
            }
        }
        return new ResourceFork(resources);
    }
}
