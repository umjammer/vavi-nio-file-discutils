
package DiscUtils.Core.CoreCompat;

public class EncodingHelper {
    private static boolean _registered;

    public static void registerEncodings() {
        if (_registered)
            return;

        _registered = true;
    }

}
