
package moe.yo3explorer.dotnetio4j;

public enum AccessControlSections {
    /** 随意アクセス制御リスト (DACL: Discretionary Access Control List)。 */
    Access(0x2),
    /** セキュリティ記述子全体。 */
    All(0xF),
    /** システム アクセス制御リスト (SACL: System Access Control List)。 */
    Audit(0x1),
    /** プライマリ グループ。 */
    Group(0x8),
    /** セクションを指定しません。 */
    None(0x0),
    /** 所有者。 */
    Owner(0x4);
    int value;
    AccessControlSections(int value) {
        this.value = value;
    }
}
