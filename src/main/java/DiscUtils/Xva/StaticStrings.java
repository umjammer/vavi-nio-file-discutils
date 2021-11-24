
package DiscUtils.Xva;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.IntStream;

public class StaticStrings {
    // @formatter:off
    public static final String XVA_ova_base =
        "<value>" +
        "  <struct>" +
        "    <member>" +
        "      <name>version</name>" +
        "      <value>" +
        "        <struct>" +
        "          <member>" +
        "            <name>hostname</name>" +
        "            <value>cheesy-2</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>date</name>" +
        "            <value>2008-11-08</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>product_version</name>" +
        "            <value>1.0.0</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>product_brand</name>" +
        "            <value>DiscUtils .NET</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>build_number</name>" +
        "            <value>0</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>xapi_major</name>" +
        "            <value>1</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>xapi_minor</name>" +
        "            <value>2</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>export_vsn</name>" +
        "            <value>2</value>" +
        "          </member>" +
        "        </struct>" +
        "      </value>" +
        "    </member>" +
        "    <member>" +
        "      <name>objects</name>" +
        "      <value>" +
        "        <array>" +
        "          <data>" +
        "            %s" +
        "          </data>" +
        "        </array>" +
        "      </value>" +
        "    </member>" +
        "  </struct>" +
        "</value>";

    public static final String XVA_ova_ref =
        "<value>" +
        "  <struct>" +
        "    <member>" +
        "      <name>version</name>" +
        "      <value>" +
        "        <struct>" +
        "          <member>" +
        "            <name>hostname</name>" +
        "            <value>cheesy-2</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>date</name>" +
        "            <value>2008-11-08</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>product_version</name>" +
        "            <value>1.0.0</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>product_brand</name>" +
        "            <value>DiscUtils .NET</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>build_number</name>" +
        "            <value>0</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>xapi_major</name>" +
        "            <value>1</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>xapi_minor</name>" +
        "            <value>2</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>export_vsn</name>" +
        "            <value>2</value>" +
        "          </member>" +
        "        </struct>" +
        "      </value>" +
        "    </member>" +
        "    <member>" +
        "      <name>objects</name>" +
        "      <value>" +
        "        <array>" +
        "          <data>" +
        "            %s" +
        "          </data>" +
        "        </array>" +
        "      </value>" +
        "    </member>" +
        "  </struct>" +
        "</value>";

    public static final String XVA_ova_vbd =
        "<value>" +
        "  <struct>" +
        "    <member>" +
        "      <name>class</name>" +
        "      <value>VBD</value>" +
        "    </member>" +
        "    <member>" +
        "      <name>id</name>" +
        "      <value>%s</value>" +
        "    </member>" +
        "    <member>" +
        "      <name>snapshot</name>" +
        "      <value>" +
        "        <struct>" +
        "          <member>" +
        "            <name>uuid</name>" +
        "            <value>%s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>allowed_operations</name>" +
        "            <value>" +
        "              <array>" +
        "                <data>" +
        "                  <value>pause</value>" +
        "                  <value>unpause</value>" +
        "                  <value>attach</value>" +
        "                </data>" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>current_operations</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VM</name>" +
        "            <value>%s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VDI</name>" +
        "            <value>%s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>device</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>userdevice</name>" +
        "            <value>%s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>bootable</name>" +
        "            <value>" +
        "              <boolean>1</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>mode</name>" +
        "            <value>RW</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>type</name>" +
        "            <value>Disk</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>unpluggable</name>" +
        "            <value>" +
        "              <boolean>1</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>storage_lock</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>empty</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>other_config</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>currently_attached</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>status_code</name>" +
        "            <value>0</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>status_detail</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>runtime_properties</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>qos_algorithm_type</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>qos_algorithm_params</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>qos_supported_algorithms</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>metrics</name>" +
        "            <value>OpaqueRef:NULL</value>" +
        "          </member>" +
        "        </struct>" +
        "      </value>" +
        "    </member>" +
        "  </struct>" +
        "</value>";

    public static final String XVA_ova_vm =
        "<value>" +
        "  <struct>" +
        "    <member>" +
        "      <name>class</name>" +
        "      <value>VM</value>" +
        "    </member>" +
        "    <member>" +
        "      <name>id</name>" +
        "      <value>%s</value>" +
        "    </member>" +
        "    <member>" +
        "      <name>snapshot</name>" +
        "      <value>" +
        "        <struct>" +
        "          <member>" +
        "            <name>uuid</name>" +
        "            <value>%s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>allowed_operations</name>" +
        "            <value>" +
        "              <array>" +
        "                <data>" +
        "                  <value>copy</value>" +
        "                  <value>clone</value>" +
        "                  <value>export</value>" +
        "                </data>" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>current_operations</name>" +
        "            <value>" +
        "              <struct>" +
        "                <member>" +
        "                  <name>OpaqueRef:b3215dfd-1735-ac53-5f26-599ac1f29a7e</name>" +
        "                  <value>export</value>" +
        "                </member>" +
        "              </struct>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>power_state</name>" +
        "            <value>Halted</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>name_label</name>" +
        "            <value>%s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>name_description</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>user_version</name>" +
        "            <value>1</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>is_a_template</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>suspend_VDI</name>" +
        "            <value>OpaqueRef:NULL</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>resident_on</name>" +
        "            <value>OpaqueRef:NULL</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>affinity</name>" +
        "            <value>OpaqueRef:80629459-b051-4e03-a43e-63c88329fcd0</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>memory_target</name>" +
        "            <value>268435456</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>memory_static_max</name>" +
        "            <value>268435456</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>memory_dynamic_max</name>" +
        "            <value>268435456</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>memory_dynamic_min</name>" +
        "            <value>268435456</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>memory_static_min</name>" +
        "            <value>16777216</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VCPUs_params</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VCPUs_max</name>" +
        "            <value>1</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VCPUs_at_startup</name>" +
        "            <value>1</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>actions_after_shutdown</name>" +
        "            <value>destroy</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>actions_after_reboot</name>" +
        "            <value>restart</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>actions_after_crash</name>" +
        "            <value>restart</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>consoles</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VIFs</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VBDs</name>" +
        "            <value>" +
        "              <array>" +
        "                <data>" +
        "                  %s" +
        "                </data>" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>crash_dumps</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VTPMs</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>PV_bootloader</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>PV_kernel</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>PV_ramdisk</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>PV_args</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>PV_bootloader_args</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>PV_legacy_args</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>HVM_boot_policy</name>" +
        "            <value>BIOS order</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>HVM_boot_params</name>" +
        "            <value>" +
        "              <struct>" +
        "                <member>" +
        "                  <name>order</name>" +
        "                  <value>dc</value>" +
        "                </member>" +
        "              </struct>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>HVM_shadow_multiplier</name>" +
        "            <value>" +
        "              <double>1</double>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>platform</name>" +
        "            <value>" +
        "              <struct>" +
        "                <member>" +
        "                  <name>timeoffset</name>" +
        "                  <value>0</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>nx</name>" +
        "                  <value>false</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>acpi</name>" +
        "                  <value>true</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>apic</name>" +
        "                  <value>true</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>pae</name>" +
        "                  <value>true</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>viridian</name>" +
        "                  <value>true</value>" +
        "                </member>" +
        "              </struct>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>PCI_bus</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>other_config</name>" +
        "            <value>" +
        "              <struct>" +
        "                <member>" +
        "                  <name>last_shutdown_time</name>" +
        "                  <value>19700101T00:00:00Z</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>last_shutdown_action</name>" +
        "                  <value>Destroy</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>last_shutdown_initiator</name>" +
        "                  <value>external</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>last_shutdown_reason</name>" +
        "                  <value>halted</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>install-methods</name>" +
        "                  <value>cdrom</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>mac_seed</name>" +
        "                  <value>9deed486-a5d6-3055-d322-81167e63fa25</value>" +
        "                </member>" +
        "              </struct>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>domid</name>" +
        "            <value>-1</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>domarch</name>" +
        "            <value>hvm</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>last_boot_CPU_flags</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>is_control_domain</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>metrics</name>" +
        "            <value>OpaqueRef:NULL</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>guest_metrics</name>" +
        "            <value>OpaqueRef:NULL</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>last_booted_record</name>" +
        "            <value></value>" +
        "          </member>" +
        "          <member>" +
        "            <name>recommendations</name>" +
        "            <value></value>" +
        "          </member>" +
        "          <member>" +
        "            <name>xenstore_data</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>ha_always_run</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>ha_restart_priority</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>is_a_snapshot</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>snapshot_of</name>" +
        "            <value>OpaqueRef:NULL</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>snapshots</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>snapshot_time</name>" +
        "            <value>" +
        "              <dateTime.iso8601>19700101T00:00:00Z</dateTime.iso8601>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>transportable_snapshot_id</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>blobs</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>tags</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>blocked_operations</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "        </struct>" +
        "      </value>" +
        "    </member>" +
        "  </struct>" +
        "</value>";

    public static final String XVA_ova_vdi =
        "<value>" +
        "  <struct>" +
        "    <member>" +
        "      <name>class</name>" +
        "      <value>VDI</value>" +
        "    </member>" +
        "    <member>" +
        "      <name>id</name>" +
        "      <value>%1$s</value>" +
        "    </member>" +
        "    <member>" +
        "      <name>snapshot</name>" +
        "      <value>" +
        "        <struct>" +
        "          <member>" +
        "            <name>uuid</name>" +
        "            <value>%2$s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>name_label</name>" +
        "            <value>%3$s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>name_description</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>allowed_operations</name>" +
        "            <value>" +
        "              <array>" +
        "                <data>" +
        "                  <value>clone</value>" +
        "                  <value>destroy</value>" +
        "                  <value>update</value>" +
        "                </data>" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>current_operations</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>SR</name>" +
        "            <value>%4$s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VBDs</name>" +
        "            <value>" +
        "              <array>" +
        "                <data>" +
        "                  <value>%5$s</value>" +
        "                </data>" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>crash_dumps</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>virtual_size</name>" +
        "            <value>%6$s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>physical_utilisation</name>" +
        "            <value>5120</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>type</name>" +
        "            <value>user</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>sharable</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>read_only</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>other_config</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>storage_lock</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>location</name>" +
        "            <value>%2$s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>managed</name>" +
        "            <value>" +
        "              <boolean>1</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>missing</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>parent</name>" +
        "            <value>OpaqueRef:NULL</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>xenstore_data</name>" +
        "            <value>" +
        "              <struct>" +
        "                <member>" +
        "                  <name>scsi/0x12/0x83</name>" +
        "                  <value>AIMAMQIBAC1YRU5TUkMgIDhhYzIzYTE2LWI5NWUtNDQ4YS04ZTA5LWY5ZmU0NTEyYTk4NCA=</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>scsi/0x12/0x80</name>" +
        "                  <value>AIAAJjhhYzIzYTE2LWI5NWUtNDQ4YS04ZTA5LWY5ZmU0NTEyYTk4NCAg</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>vdi-uuid</name>" +
        "                  <value>%2$s</value>" +
        "                </member>" +
        "                <member>" +
        "                  <name>storage-type</name>" +
        "                  <value>ext</value>" +
        "                </member>" +
        "              </struct>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>sm_config</name>" +
        "            <value>" +
        "              <struct>" +
        "                <member>" +
        "                  <name>vhd-parent</name>" +
        "                  <value>7fe09881-8bff-4138-8381-adebcd968786</value>" +
        "                </member>" +
        "              </struct>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>is_a_snapshot</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>snapshot_of</name>" +
        "            <value>OpaqueRef:NULL</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>snapshots</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>snapshot_time</name>" +
        "            <value>" +
        "              <dateTime.iso8601>19700101T00:00:00Z</dateTime.iso8601>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>tags</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "        </struct>" +
        "      </value>" +
        "    </member>" +
        "  </struct>" +
        "</value>";

    public static String XVA_ova_sr =
        "<value>" +
        "  <struct>" +
        "    <member>" +
        "      <name>class</name>" +
        "      <value>SR</value>" +
        "    </member>" +
        "    <member>" +
        "      <name>id</name>" +
        "      <value>%s</value>" +
        "    </member>" +
        "    <member>" +
        "      <name>snapshot</name>" +
        "      <value>" +
        "        <struct>" +
        "          <member>" +
        "            <name>uuid</name>" +
        "            <value>%s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>name_label</name>" +
        "            <value>%s</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>name_description</name>" +
        "            <value />" +
        "          </member>" +
        "          <member>" +
        "            <name>allowed_operations</name>" +
        "            <value>" +
        "              <array>" +
        "                <data>" +
        "                  <value>forget</value>" +
        "                  <value>vdi_create</value>" +
        "                  <value>vdi_snapshot</value>" +
        "                  <value>plug</value>" +
        "                  <value>destroy</value>" +
        "                  <value>vdi_destroy</value>" +
        "                  <value>scan</value>" +
        "                  <value>vdi_clone</value>" +
        "                  <value>unplug</value>" +
        "                </data>" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>current_operations</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>VDIs</name>" +
        "            <value>" +
        "              <array>" +
        "                <data>" +
        "                  %s" +
        "                </data>" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>PBDs</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>virtual_allocation</name>" +
        "            <value>111929556992</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>physical_utilisation</name>" +
        "            <value>29303386112</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>physical_size</name>" +
        "            <value>237981646848</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>type</name>" +
        "            <value>ext</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>content_type</name>" +
        "            <value>user</value>" +
        "          </member>" +
        "          <member>" +
        "            <name>shared</name>" +
        "            <value>" +
        "              <boolean>0</boolean>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>other_config</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>tags</name>" +
        "            <value>" +
        "              <array>" +
        "                <data />" +
        "              </array>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>sm_config</name>" +
        "            <value>" +
        "              <struct>" +
        "                <member>" +
        "                  <name>devserial</name>" +
        "                  <value>scsi-SATA_ST3250820AS_9QE5DQ7M</value>" +
        "                </member>" +
        "              </struct>" +
        "            </value>" +
        "          </member>" +
        "          <member>" +
        "            <name>blobs</name>" +
        "            <value>" +
        "              <struct />" +
        "            </value>" +
        "          </member>" +
        "        </struct>" +
        "      </value>" +
        "    </member>" +
        "  </struct>" +
        "</value>";
    // @formatter:on

    public static void main(String[] args) throws Exception {
        String[] names = { "XVA_ova_base", "XVA_ova_ref", "XVA_ova_vbd", "XVA_ova_vm", "XVA_ova_vdi", "XVA_ova_sr" };
        String[] values = { XVA_ova_base, XVA_ova_ref, XVA_ova_vbd, XVA_ova_vm, XVA_ova_vdi, XVA_ova_sr };
        IntStream.range(0, values.length).forEach(i -> {
                try {
                    Files.write(Paths.get("src/main/resources", names[i] + ".xml"), values[i].getBytes());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
    }
}
