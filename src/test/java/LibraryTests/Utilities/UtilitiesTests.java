//

package LibraryTests.Utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Core.Internal.Utilities;


public class UtilitiesTests {

    @Test
    public void canResolveRelativePath() throws Exception {
        checkResolvePath("\\etc\\rc.d", "init.d", "\\etc\\init.d");
        checkResolvePath("\\etc\\rc.d\\", "init.d", "\\etc\\rc.d\\init.d");
        // For example: (\TEMP\Foo.txt, ..\..\Bar.txt) gives (\Bar.txt).
        checkResolvePath("\\TEMP\\Foo.txt", "..\\..\\Bar.txt", "\\Bar.txt");
    }

    private void checkResolvePath(String basePath, String relativePath, String expectedResult) throws Exception {
        String result = Utilities.resolvePath(basePath, relativePath);
        assertEquals(expectedResult, result);
    }
}
