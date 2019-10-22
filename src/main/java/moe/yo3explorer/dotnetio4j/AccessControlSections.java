
package moe.yo3explorer.dotnetio4j;

import java.util.EnumSet;


public enum AccessControlSections {
    /** 随意アクセス制御リスト (DACL: Discretionary Access Control List)。 */
    Access(0x2),
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

    /** セキュリティ記述子全体。 */
    public static final EnumSet<AccessControlSections> All = EnumSet.of(Audit, Access, Owner, Group);
}
