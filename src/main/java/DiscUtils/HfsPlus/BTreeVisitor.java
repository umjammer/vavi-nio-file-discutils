
package DiscUtils.HfsPlus;

@FunctionalInterface
interface BTreeVisitor<Key extends BTreeKey<?>> {

    int invoke(Key key, byte[] data);
}
