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

package DiscUtils.Iso9660;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.Util.StreamUtilities;


public final class SuspRecords {
    private final Map<String, Map<String, List<SystemUseEntry>>> _records;

    public SuspRecords(IsoContext context, byte[] data, int offset) {
        _records = new HashMap<>();
        ContinuationSystemUseEntry contEntry = parse(context, data, offset + context.getSuspSkipBytes());
        while (contEntry != null) {
            context.getDataStream()
                    .setPosition(contEntry.Block * (long) context.getVolumeDescriptor().LogicalBlockSize +
                                 contEntry.BlockOffset);
            byte[] contData = StreamUtilities.readExact(context.getDataStream(), contEntry.Length);
            contEntry = parse(context, contData, 0);
        }
    }

    public static boolean detectSharingProtocol(byte[] data, int offset) {
        if (data == null || data.length - offset < 7) {
            return false;
        }

        return data[offset] == 83 && data[offset + 1] == 80 && data[offset + 2] == 7 && data[offset + 3] == 1 &&
               data[offset + 4] == 0xBE && data[offset + 5] == 0xEF;
    }

    public List<SystemUseEntry> getEntries(String extension, String name) {
        if (extension == null || extension.isEmpty()) {
            extension = "";
        }

        if (!_records.containsKey(extension)) {
            return null;
        }
        Map<String, List<SystemUseEntry>> extensionData = _records.get(extension);

        if (extensionData.containsKey(name)) {
            List<SystemUseEntry> result = extensionData.get(name);
            return result;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends SystemUseEntry> T getEntry(String extension, String name) {
        List<SystemUseEntry> entries = getEntries(extension, name);
        if (entries == null) {
            return null;
        }

        for (SystemUseEntry entry : entries) {
            return (T) entry;
        }
        return null;
    }

    public boolean hasEntry(String extension, String name) {
        List<SystemUseEntry> entries = getEntries(extension, name);
        return entries != null && entries.size() != 0;
    }

    private ContinuationSystemUseEntry parse(IsoContext context, byte[] data, int offset) {
        ContinuationSystemUseEntry contEntry = null;
        SuspExtension extension = null;
        if (context.getSuspExtensions() != null && context.getSuspExtensions().size() > 0) {
            extension = context.getSuspExtensions().get(0);
        }

        int pos = offset;
        while (data.length - pos > 4) {
            byte[] len = new byte[1];
            SystemUseEntry entry = SystemUseEntry
                    .parse(data, pos, context.getVolumeDescriptor().CharacterEncoding, extension, len);
            pos += len[0];
            if (entry == null) {
                return contEntry;
            }

            // A null entry indicates SUSP parsing must terminate.
            // This will occur if a termination record is found,
            // or if there is a problem with the SUSP data.
            String __dummyScrutVar0 = entry.Name;
            if (__dummyScrutVar0.equals("CE")) {
                contEntry = (ContinuationSystemUseEntry) entry;
            } else if (__dummyScrutVar0.equals("ES")) {
                ExtensionSelectSystemUseEntry esEntry = (ExtensionSelectSystemUseEntry) entry;
                extension = context.getSuspExtensions().get(esEntry.SelectedExtension);
            } else if (__dummyScrutVar0.equals("PD")) {
            } else if (__dummyScrutVar0.equals("SP") || __dummyScrutVar0.equals("ER")) {
                storeEntry(null, entry);
            } else {
                storeEntry(extension, entry);
            }
        }
        return contEntry;
    }

    private void storeEntry(SuspExtension extension, SystemUseEntry entry) {
        String extensionId = extension == null ? "" : extension.getIdentifier();
        Map<String, List<SystemUseEntry>> extensionEntries;
        if (!_records.containsKey(extensionId)) {
            extensionEntries = new HashMap<>();
            _records.put(extensionId, extensionEntries);
        }
        extensionEntries = _records.get(extensionId);

        List<SystemUseEntry> entries;
        if (!extensionEntries.containsKey(entry.Name)) {
            entries = new ArrayList<>();
            extensionEntries.put(entry.Name, entries);
        }
        entries = extensionEntries.get(entry.Name);

        entries.add(entry);
    }
}
