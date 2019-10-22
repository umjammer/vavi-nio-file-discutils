
package DiscUtils.HfsPlus;

@FunctionalInterface
public interface BTreeVisitor<Key extends BTreeKey<?>> {

    int invoke(Key key, byte[] data);
}
