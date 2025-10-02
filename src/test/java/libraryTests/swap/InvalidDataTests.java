//

package libraryTests.swap;

import discUtils.swap.SwapHeader;
import org.junit.jupiter.api.Test;


class InvalidDataTests {
    @Test
    void invalidSwapHeader() throws Exception {
        byte[] buffer = new byte[SwapHeader.PageSize];
        for (int i = 0; i < 16; i++) {
            buffer[0x41c + i] = 1;
        }
        SwapHeader header = new SwapHeader();
        header.readFrom(buffer, 0);
    }
}
