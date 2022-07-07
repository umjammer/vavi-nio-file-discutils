
package discUtils.squashFs;

import discUtils.streams.block.Block;


@FunctionalInterface
public interface ReadBlock {

    Block invoke(long pos, int diskLen);
}
