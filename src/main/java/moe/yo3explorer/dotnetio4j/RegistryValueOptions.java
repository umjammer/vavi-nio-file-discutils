
package moe.yo3explorer.dotnetio4j;

public enum RegistryValueOptions {
    /** オプションの動作は指定されていません。 */
    None,
    /**
     * 型の値が、埋め込まれた環境変数を展開せずに取得されます。
     *
     * @see "F:Microsoft.Win32.RegistryValueKind.ExpandString"
     */
    DoNotExpandEnvironmentNames;
}
