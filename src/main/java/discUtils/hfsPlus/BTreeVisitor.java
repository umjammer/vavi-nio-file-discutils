
package discUtils.hfsPlus;

@FunctionalInterface
interface BTreeVisitor<Key extends BTreeKey<?>> {

    int invoke(Key key, byte[] data);
}
