//

package libraryTests.utilities;

import java.io.File;

import discUtils.core.internal.Utilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class UtilitiesTests {

    private static final String FS = File.separator;

    @Test
    void canResolveRelativePath() throws Exception {
        checkResolvePath(FS + "etc" + FS + "rc.d", "init.d", FS + "etc" + FS + "init.d");
        checkResolvePath(FS + "etc" + FS + "rc.d" + FS, "init.d", FS + "etc" + FS + "rc.d" + FS + "init.d");
        // For example: (\TEMP\Foo.txt, ..\..\Bar.txt) gives (\Bar.txt).
        checkResolvePath(FS + "TEMP" + FS + "Foo.txt", ".." + FS + ".." + FS + "Bar.txt", FS + "Bar.txt");
    }

    private static void checkResolvePath(String basePath, String relativePath, String expectedResult) {
        String result = Utilities.resolvePath(basePath, relativePath);
        assertEquals(expectedResult, result);
    }
}
