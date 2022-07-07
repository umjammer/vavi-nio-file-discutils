
package discUtils.squashFs;

@FunctionalInterface
public interface ReadMetaBlock {

    Metablock invoke(long pos);
}
