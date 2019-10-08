
package DiscUtils.SquashFs;

import DiscUtils.Streams.Block.Block;


@FunctionalInterface
public interface ReadBlock {

    Block invoke(long pos, int diskLen);
}
