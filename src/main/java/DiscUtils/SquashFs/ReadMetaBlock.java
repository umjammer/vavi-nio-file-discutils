
package DiscUtils.SquashFs;

@FunctionalInterface
public interface ReadMetaBlock {

    Metablock invoke(long pos);
}
