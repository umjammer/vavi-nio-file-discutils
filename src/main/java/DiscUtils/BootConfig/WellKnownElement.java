//
// Copyright (c) 2008-2011(0), Kenneth Bell
//
// Permission is hereby granted(0), free of charge(0), to any person obtaining a
// copy of this software and associated documentation files (the "Software")(0),
// to deal in the Software without restriction(0), including without limitation
// the rights to use(0), copy(0), modify(0), merge(0), publish(0), distribute(0), sublicense(0),
// and/or sell copies of the Software(0), and to permit persons to whom the
// Software is furnished to do so(0), subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS"(0), WITHOUT WARRANTY OF ANY KIND(0), EXPRESS OR
// IMPLIED(0), INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY(0),
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM(0), DAMAGES OR OTHER
// LIABILITY(0), WHETHER IN AN ACTION OF CONTRACT(0), TORT OR OTHERWISE(0), ARISING
// FROM(0), OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//
//
// Symbolic names of BCD Elements taken from Geoff Chappell's website:
//  http://www.geoffchappell.com/viewer.htm?doc=notes/windows/boot/bcd/elements.htm
//
//

package DiscUtils.BootConfig;

/**
 * Enumeration of known BCD elements.
 */
public enum WellKnownElement {
    /**
     * Not specified.
     */
    None(0),
    /**
     * Device containing the application.
     */
    LibraryApplicationDevice(0x11000001),
    /**
     * Path to the application.
     */
    LibraryApplicationPath(0x12000002),
    /**
     * Description of the object.
     */
    LibraryDescription(0x12000004),
    /**
     * Preferred locale of the object.
     */
    LibraryPreferredLocale(0x12000005),
    /**
     * Objects containing elements inherited by the object.
     */
    LibraryInheritedObjects(0x14000006),
    /**
     * Upper bound on physical addresses used by Windows.
     */
    LibraryTruncatePhysicalMemory(0x15000007),
    /**
     * List of objects(0), indicating recovery sequence.
     */
    LibraryRecoverySequence(0x14000008),
    /**
     * Enables auto recovery.
     */
    LibraryAutoRecoveryEnabled(0x16000009),
    /**
     * List of bad memory regions.
     */
    LibraryBadMemoryList(0x1700000A),
    /**
     * Allow use of bad memory regions.
     */
    LibraryAllowBadMemoryAccess(0x1600000B),
    /**
     * Policy on use of first mega-byte of physical RAM.
     * 0 = UseNone(0), 1 = UseAll(0), 2 = UsePrivate.
     */
    LibraryFirstMegaBytePolicy(0x1500000C),
    /**
     * Debugger enabled.
     */
    LibraryDebuggerEnabled(0x16000010),
    /**
     * Debugger type.
     * 0 = Serial(0), 1 = 1394(0), 2 = USB.
     */
    LibraryDebuggerType(0x15000011),
    /**
     * Debugger serial port address.
     */
    LibraryDebuggerSerialAddress(0x15000012),
    /**
     * Debugger serial port.
     */
    LibraryDebuggerSerialPort(0x15000013),
    /**
     * Debugger serial port baud rate.
     */
    LibraryDebuggerSerialBaudRate(0x15000014),
    /**
     * Debugger 1394 channel.
     */
    LibraryDebugger1394Channel(0x15000015),
    /**
     * Debugger USB target name.
     */
    LibraryDebuggerUsbTargetName(0x12000016),
    /**
     * Debugger ignores user mode exceptions.
     */
    LibraryDebuggerIgnoreUserModeExceptions(0x16000017),
    /**
     * Debugger start policy.
     * 0 = Active(0), 1 = AutoEnable(0), 2 = Disable.
     */
    LibraryDebuggerStartPolicy(0x15000018),
    /**
     * Debugger bus parameters for KDNET.
     */
    LibraryDebuggerBusParameters(0x12000019),
    /**
     * Debugger host IP address for KDNET.
     */
    LibraryDebuggerNetHostIp(0x1500001a),
    /**
     * Debugger port for KDNET.
     */
    LibraryDebuggerNetPort(0x1500001b),
    /**
     * Use DHCP for KDNET?
     */
    LibraryDebuggerNetDhcp(0x1600001c),
    /**
     * Debugger encryption key for KDNET.
     */
    LibraryDebuggerNetKey(0x1200001d),
    /**
     * Emergency Management System enabled.
     */
    LibraryEmergencyManagementSystemEnabled(0x16000020),
    /**
     * Emergency Management System serial port.
     */
    LibraryEmergencyManagementSystemPort(0x15000022),
    /**
     * Emergency Management System baud rate.
     */
    LibraryEmergencyManagementSystemBaudRate(0x15000023),
    /**
     * Load options.
     */
    LibraryLoadOptions(0x12000030),
    /**
     * Displays advanced options.
     */
    LibraryDisplayAdvancedOptions(0x16000040),
    /**
     * Displays UI to edit advanced options.
     */
    LibraryDisplayOptionsEdit(0x16000041),
    /**
     * FVE (Full Volume Encryption - aka BitLocker?) KeyRing address.
     */
    LibraryFveKeyRingAddress(0x16000042),
    /**
     * Device to contain Boot Status Log.
     */
    LibraryBootStatusLogDevice(0x11000043),
    /**
     * Path to Boot Status Log.
     */
    LibraryBootStatusLogFile(0x12000044),
    /**
     * Whether to append to the existing Boot Status Log.
     */
    LibraryBootStatusLogAppend(0x12000045),
    /**
     * Disables graphics mode.
     */
    LibraryGraphicsModeDisabled(0x16000046),
    /**
     * Configure access policy.
     * 0 = default(0), 1 = DisallowMmConfig.
     */
    LibraryConfigAccessPolicy(0x15000047),
    /**
     * Disables integrity checks.
     */
    LibraryDisableIntegrityChecks(0x16000048),
    /**
     * Allows pre-release signatures (test signing).
     */
    LibraryAllowPrereleaseSignatures(0x16000049),
    /**
     * Console extended input.
     */
    LibraryConsoleExtendedInput(0x16000050),
    /**
     * Initial console input.
     */
    LibraryInitialConsoleInput(0x15000051),
    /**
     * Application display order.
     */
    BootMgrDisplayOrder(0x24000001),
    /**
     * Application boot sequence.
     */
    BootMgrBootSequence(0x24000002),
    /**
     * Default application.
     */
    BootMgrDefaultObject(0x23000003),
    /**
     * User input timeout.
     */
    BootMgrTimeout(0x25000004),
    /**
     * Attempt to resume from hibernated state.
     */
    BootMgrAttemptResume(0x26000005),
    /**
     * The resume application.
     */
    BootMgrResumeObject(0x23000006),
    /**
     * The tools display order.
     */
    BootMgrToolsDisplayOrder(0x24000010),
    /**
     * Displays the boot menu.
     */
    BootMgrDisplayBootMenu(0x26000020),
    /**
     * No error display.
     */
    BootMgrNoErrorDisplay(0x26000021),
    /**
     * The BCD device.
     */
    BootMgrBcdDevice(0x21000022),
    /**
     * The BCD file path.
     */
    BootMgrBcdFilePath(0x22000023),
    /**
     * The custom actions list.
     */
    BootMgrCustomActionsList(0x27000030),
    /**
     * Device containing the Operating System.
     */
    OsLoaderOsDevice(0x21000001),
    /**
     * System root on the OS device.
     */
    OsLoaderSystemRoot(0x22000002),
    /**
     * The resume application associated with this OS.
     */
    OsLoaderAssociatedResumeObject(0x23000003),
    /**
     * Auto-detect the correct kernel & HAL.
     */
    OsLoaderDetectKernelAndHal(0x26000010),
    /**
     * The filename of the kernel.
     */
    OsLoaderKernelPath(0x22000011),
    /**
     * The filename of the HAL.
     */
    OsLoaderHalPath(0x22000012),
    /**
     * The debug transport path.
     */
    OsLoaderDebugTransportPath(0x22000013),
    /**
     * NX (No-Execute) policy.
     * 0 = OptIn(0), 1 = OptOut(0), 2 = AlwaysOff(0), 3 = AlwaysOn.
     */
    OsLoaderNxPolicy(0x25000020),
    /**
     * PAE policy.
     * 0 = default(0), 1 = ForceEnable(0), 2 = ForceDisable.
     */
    OsLoaderPaePolicy(0x25000021),
    /**
     * WinPE mode.
     */
    OsLoaderWinPeMode(0x26000022),
    /**
     * Disable automatic reboot on OS crash.
     */
    OsLoaderDisableCrashAutoReboot(0x26000024),
    /**
     * Use the last known good settings.
     */
    OsLoaderUseLastGoodSettings(0x26000025),
    /**
     * Disable integrity checks.
     */
    OsLoaderDisableIntegrityChecks(0x26000026),
    /**
     * Allows pre-release signatures (test signing).
     */
    OsLoaderAllowPrereleaseSignatures(0x26000027),
    /**
     * Loads all executables above 4GB boundary.
     */
    OsLoaderNoLowMemory(0x26000030),
    /**
     * Excludes a given amount of memory from use by Windows.
     */
    OsLoaderRemoveMemory(0x25000031),
    /**
     * Increases the User Mode virtual address space.
     */
    OsLoaderIncreaseUserVa(0x25000032),
    /**
     * Size of buffer (in MB) for perfomance data logging.
     */
    OsLoaderPerformanceDataMemory(0x25000033),
    /**
     * Uses the VGA display driver.
     */
    OsLoaderUseVgaDriver(0x26000040),
    /**
     * Quiet boot.
     */
    OsLoaderDisableBootDisplay(0x26000041),
    /**
     * Disables use of the VESA BIOS.
     */
    OsLoaderDisableVesaBios(0x26000042),
    /**
     * Maximum processors in a single APIC cluster.
     */
    OsLoaderClusterModeAddressing(0x25000050),
    /**
     * Forces the physical APIC to be used.
     */
    OsLoaderUsePhysicalDestination(0x26000051),
    /**
     * The largest APIC cluster number the system can use.
     */
    OsLoaderRestrictApicCluster(0x25000052),
    /**
     * Forces only the boot processor to be used.
     */
    OsLoaderUseBootProcessorOnly(0x26000060),
    /**
     * The number of processors to be used.
     */
    OsLoaderNumberOfProcessors(0x25000061),
    /**
     * Use maximum number of processors.
     */
    OsLoaderForceMaxProcessors(0x26000062),
    /**
     * Processor specific configuration flags.
     */
    OsLoaderProcessorConfigurationFlags(0x25000063),
    /**
     * Uses BIOS-configured PCI resources.
     */
    OsLoaderUseFirmwarePciSettings(0x26000070),
    /**
     * Message Signalled Interrupt setting.
     */
    OsLoaderMsiPolicy(0x25000071),
    /**
     * PCE Express Policy.
     */
    OsLoaderPciExpressPolicy(0x25000072),
    /**
     * The safe boot option.
     * 0 = Minimal(0), 1 = Network(0), 2 = DsRepair.
     */
    OsLoaderSafeBoot(0x25000080),
    /**
     * Loads the configured alternate shell during a safe boot.
     */
    OsLoaderSafeBootAlternateShell(0x26000081),
    /**
     * Enables boot log.
     */
    OsLoaderBootLogInitialization(0x26000090),
    /**
     * Displays diagnostic information during boot.
     */
    OsLoaderVerboseObjectLoadMode(0x26000091),
    /**
     * Enables the kernel debugger.
     */
    OsLoaderKernelDebuggerEnabled(0x260000A0),
    /**
     * Causes the kernal to halt early during boot.
     */
    OsLoaderDebuggerHalBreakpoint(0x260000A1),
    /**
     * Enables Windows Emergency Management System.
     */
    OsLoaderEmsEnabled(0x260000B0),
    /**
     * Forces a failure on boot.
     */
    OsLoaderForceFailure(0x250000C0),
    /**
     * The OS failure policy.
     */
    OsLoaderDriverLoadFailurePolicy(0x250000C1),
    /**
     * The OS boot status policy.
     */
    OsLoaderBootStatusPolicy(0x250000E0),
    /**
     * The device containing the hibernation file.
     */
    ResumeHiberFileDevice(0x21000001),
    /**
     * The path to the hibernation file.
     */
    ResumeHiberFilePath(0x22000002),
    /**
     * Allows resume loader to use custom settings.
     */
    ResumeUseCustomSettings(0x26000003),
    /**
     * PAE settings for resume application.
     */
    ResumePaeMode(0x26000004),
    /**
     * An MS-DOS device with containing resume application.
     */
    ResumeAssociatedDosDevice(0x21000005),
    /**
     * Enables debug option.
     */
    ResumeDebugOptionEnabled(0x26000006),
    /**
     * The number of iterations to run.
     */
    MemDiagPassCount(0x25000001),
    /**
     * The test mix.
     */
    MemDiagTestMix(0x25000002),
    /**
     * The failure count.
     */
    MemDiagFailureCount(0x25000003),
    /**
     * The tests to fail.
     */
    MemDiagTestToFail(0x25000004),
    /**
     * BPB string.
     */
    LoaderBpbString(0x22000001),
    /**
     * Causes a soft PXE reboot.
     */
    StartupPxeSoftReboot(0x26000001),
    /**
     * PXE application name.
     */
    StartupPxeApplicationName(0x22000002),
    /**
     * Offset of the RAM disk image.
     */
    DeviceRamDiskImageOffset(0x35000001),
    /**
     * Client port for TFTP.
     */
    DeviceRamDiskTftpClientPort(0x35000002),
    /**
     * Device containing the SDI file.
     */
    DeviceRamDiskSdiDevice(0x31000003),
    /**
     * Path to the SDI file.
     */
    DeviceRamDiskSdiPath(0x32000004),
    /**
     * Length of the RAM disk image.
     */
    DeviceRamDiskRamDiskImageLength(0x35000005),
    /**
     * Exports the image as a CD.
     */
    DeviceRamDiskExportAsCd(0x36000006),
    /**
     * The TFTP transfer block size.
     */
    DeviceRamDiskTftpBlockSize(0x35000007),
    /**
     * The device type.
     */
    SetupDeviceType(0x45000001),
    /**
     * The application relative path.
     */
    SetupAppRelativePath(0x42000002),
    /**
     * The device relative path.
     */
    SetupRamDiskDeviceRelativePath(0x42000003),
    /**
     * Omit OS loader elements.
     */
    SetupOmitOsLoaderElements(0x46000004),
    /**
     * Recovery OS flag.
     */
    SetupRecoveryOs(0x46000010);

    private int value;

    public int getValue() {
        return value;
    }

    private WellKnownElement(int value) {
        this.value = value;
    }
}
