//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//
//
// Symbolic names of BCD Elements taken from Geoff Chappell's website:
//  http://www.geoffchappell.com/viewer.htm?doc=notes/windows/boot/bcd/elements.htm
//
//

package DiscUtils.BootConfig;

public enum WellKnownElement {
    /**
     * Enumeration of known BCD elements.
     * 
     * Not specified.
     */
    None,
    /**
     * Device containing the application.
     */
    LibraryApplicationDevice,
    /**
     * Path to the application.
     */
    LibraryApplicationPath,
    /**
     * Description of the object.
     */
    LibraryDescription,
    /**
     * Preferred locale of the object.
     */
    LibraryPreferredLocale,
    /**
     * Objects containing elements inherited by the object.
     */
    LibraryInheritedObjects,
    /**
     * Upper bound on physical addresses used by Windows.
     */
    LibraryTruncatePhysicalMemory,
    /**
     * List of objects, indicating recovery sequence.
     */
    LibraryRecoverySequence,
    /**
     * Enables auto recovery.
     */
    LibraryAutoRecoveryEnabled,
    /**
     * List of bad memory regions.
     */
    LibraryBadMemoryList,
    /**
     * Allow use of bad memory regions.
     */
    LibraryAllowBadMemoryAccess,
    /**
     * Policy on use of first mega-byte of physical RAM.
     * 0 = UseNone, 1 = UseAll, 2 = UsePrivate.
     */
    LibraryFirstMegaBytePolicy,
    /**
     * Debugger enabled.
     */
    LibraryDebuggerEnabled,
    /**
     * Debugger type.
     * 0 = Serial, 1 = 1394, 2 = USB.
     */
    LibraryDebuggerType,
    /**
     * Debugger serial port address.
     */
    LibraryDebuggerSerialAddress,
    /**
     * Debugger serial port.
     */
    LibraryDebuggerSerialPort,
    /**
     * Debugger serial port baud rate.
     */
    LibraryDebuggerSerialBaudRate,
    /**
     * Debugger 1394 channel.
     */
    LibraryDebugger1394Channel,
    /**
     * Debugger USB target name.
     */
    LibraryDebuggerUsbTargetName,
    /**
     * Debugger ignores user mode exceptions.
     */
    LibraryDebuggerIgnoreUserModeExceptions,
    /**
     * Debugger start policy.
     * 0 = Active, 1 = AutoEnable, 2 = Disable.
     */
    LibraryDebuggerStartPolicy,
    /**
     * Debugger bus parameters for KDNET.
     */
    LibraryDebuggerBusParameters,
    /**
     * Debugger host IP address for KDNET.
     */
    LibraryDebuggerNetHostIp,
    /**
     * Debugger port for KDNET.
     */
    LibraryDebuggerNetPort,
    /**
     * Use DHCP for KDNET?
     */
    LibraryDebuggerNetDhcp,
    /**
     * Debugger encryption key for KDNET.
     */
    LibraryDebuggerNetKey,
    /**
     * Emergency Management System enabled.
     */
    LibraryEmergencyManagementSystemEnabled,
    /**
     * Emergency Management System serial port.
     */
    LibraryEmergencyManagementSystemPort,
    /**
     * Emergency Management System baud rate.
     */
    LibraryEmergencyManagementSystemBaudRate,
    /**
     * Load options.
     */
    LibraryLoadOptions,
    /**
     * Displays advanced options.
     */
    LibraryDisplayAdvancedOptions,
    /**
     * Displays UI to edit advanced options.
     */
    LibraryDisplayOptionsEdit,
    /**
     * FVE (Full Volume Encryption - aka BitLocker?) KeyRing address.
     */
    LibraryFveKeyRingAddress,
    /**
     * Device to contain Boot Status Log.
     */
    LibraryBootStatusLogDevice,
    /**
     * Path to Boot Status Log.
     */
    LibraryBootStatusLogFile,
    /**
     * Whether to append to the existing Boot Status Log.
     */
    LibraryBootStatusLogAppend,
    /**
     * Disables graphics mode.
     */
    LibraryGraphicsModeDisabled,
    /**
     * Configure access policy.
     * 0 = default, 1 = DisallowMmConfig.
     */
    LibraryConfigAccessPolicy,
    /**
     * Disables integrity checks.
     */
    LibraryDisableIntegrityChecks,
    /**
     * Allows pre-release signatures (test signing).
     */
    LibraryAllowPrereleaseSignatures,
    /**
     * Console extended input.
     */
    LibraryConsoleExtendedInput,
    /**
     * Initial console input.
     */
    LibraryInitialConsoleInput,
    /**
     * Application display order.
     */
    BootMgrDisplayOrder,
    /**
     * Application boot sequence.
     */
    BootMgrBootSequence,
    /**
     * Default application.
     */
    BootMgrDefaultObject,
    /**
     * User input timeout.
     */
    BootMgrTimeout,
    /**
     * Attempt to resume from hibernated state.
     */
    BootMgrAttemptResume,
    /**
     * The resume application.
     */
    BootMgrResumeObject,
    /**
     * The tools display order.
     */
    BootMgrToolsDisplayOrder,
    /**
     * Displays the boot menu.
     */
    BootMgrDisplayBootMenu,
    /**
     * No error display.
     */
    BootMgrNoErrorDisplay,
    /**
     * The BCD device.
     */
    BootMgrBcdDevice,
    /**
     * The BCD file path.
     */
    BootMgrBcdFilePath,
    /**
     * The custom actions list.
     */
    BootMgrCustomActionsList,
    /**
     * Device containing the Operating System.
     */
    OsLoaderOsDevice,
    /**
     * System root on the OS device.
     */
    OsLoaderSystemRoot,
    /**
     * The resume application associated with this OS.
     */
    OsLoaderAssociatedResumeObject,
    /**
     * Auto-detect the correct kernel & HAL.
     */
    OsLoaderDetectKernelAndHal,
    /**
     * The filename of the kernel.
     */
    OsLoaderKernelPath,
    /**
     * The filename of the HAL.
     */
    OsLoaderHalPath,
    /**
     * The debug transport path.
     */
    OsLoaderDebugTransportPath,
    /**
     * NX (No-Execute) policy.
     * 0 = OptIn, 1 = OptOut, 2 = AlwaysOff, 3 = AlwaysOn.
     */
    OsLoaderNxPolicy,
    /**
     * PAE policy.
     * 0 = default, 1 = ForceEnable, 2 = ForceDisable.
     */
    OsLoaderPaePolicy,
    /**
     * WinPE mode.
     */
    OsLoaderWinPeMode,
    /**
     * Disable automatic reboot on OS crash.
     */
    OsLoaderDisableCrashAutoReboot,
    /**
     * Use the last known good settings.
     */
    OsLoaderUseLastGoodSettings,
    /**
     * Disable integrity checks.
     */
    OsLoaderDisableIntegrityChecks,
    /**
     * Allows pre-release signatures (test signing).
     */
    OsLoaderAllowPrereleaseSignatures,
    /**
     * Loads all executables above 4GB boundary.
     */
    OsLoaderNoLowMemory,
    /**
     * Excludes a given amount of memory from use by Windows.
     */
    OsLoaderRemoveMemory,
    /**
     * Increases the User Mode virtual address space.
     */
    OsLoaderIncreaseUserVa,
    /**
     * Size of buffer (in MB) for perfomance data logging.
     */
    OsLoaderPerformanceDataMemory,
    /**
     * Uses the VGA display driver.
     */
    OsLoaderUseVgaDriver,
    /**
     * Quiet boot.
     */
    OsLoaderDisableBootDisplay,
    /**
     * Disables use of the VESA BIOS.
     */
    OsLoaderDisableVesaBios,
    /**
     * Maximum processors in a single APIC cluster.
     */
    OsLoaderClusterModeAddressing,
    /**
     * Forces the physical APIC to be used.
     */
    OsLoaderUsePhysicalDestination,
    /**
     * The largest APIC cluster number the system can use.
     */
    OsLoaderRestrictApicCluster,
    /**
     * Forces only the boot processor to be used.
     */
    OsLoaderUseBootProcessorOnly,
    /**
     * The number of processors to be used.
     */
    OsLoaderNumberOfProcessors,
    /**
     * Use maximum number of processors.
     */
    OsLoaderForceMaxProcessors,
    /**
     * Processor specific configuration flags.
     */
    OsLoaderProcessorConfigurationFlags,
    /**
     * Uses BIOS-configured PCI resources.
     */
    OsLoaderUseFirmwarePciSettings,
    /**
     * Message Signalled Interrupt setting.
     */
    OsLoaderMsiPolicy,
    /**
     * PCE Express Policy.
     */
    OsLoaderPciExpressPolicy,
    /**
     * The safe boot option.
     * 0 = Minimal, 1 = Network, 2 = DsRepair.
     */
    OsLoaderSafeBoot,
    /**
     * Loads the configured alternate shell during a safe boot.
     */
    OsLoaderSafeBootAlternateShell,
    /**
     * Enables boot log.
     */
    OsLoaderBootLogInitialization,
    /**
     * Displays diagnostic information during boot.
     */
    OsLoaderVerboseObjectLoadMode,
    /**
     * Enables the kernel debugger.
     */
    OsLoaderKernelDebuggerEnabled,
    /**
     * Causes the kernal to halt early during boot.
     */
    OsLoaderDebuggerHalBreakpoint,
    /**
     * Enables Windows Emergency Management System.
     */
    OsLoaderEmsEnabled,
    /**
     * Forces a failure on boot.
     */
    OsLoaderForceFailure,
    /**
     * The OS failure policy.
     */
    OsLoaderDriverLoadFailurePolicy,
    /**
     * The OS boot status policy.
     */
    OsLoaderBootStatusPolicy,
    /**
     * The device containing the hibernation file.
     */
    ResumeHiberFileDevice,
    /**
     * The path to the hibernation file.
     */
    ResumeHiberFilePath,
    /**
     * Allows resume loader to use custom settings.
     */
    ResumeUseCustomSettings,
    /**
     * PAE settings for resume application.
     */
    ResumePaeMode,
    /**
     * An MS-DOS device with containing resume application.
     */
    ResumeAssociatedDosDevice,
    /**
     * Enables debug option.
     */
    ResumeDebugOptionEnabled,
    /**
     * The number of iterations to run.
     */
    MemDiagPassCount,
    /**
     * The test mix.
     */
    MemDiagTestMix,
    /**
     * The failure count.
     */
    MemDiagFailureCount,
    /**
     * The tests to fail.
     */
    MemDiagTestToFail,
    /**
     * BPB string.
     */
    LoaderBpbString,
    /**
     * Causes a soft PXE reboot.
     */
    StartupPxeSoftReboot,
    /**
     * PXE application name.
     */
    StartupPxeApplicationName,
    /**
     * Offset of the RAM disk image.
     */
    DeviceRamDiskImageOffset,
    /**
     * Client port for TFTP.
     */
    DeviceRamDiskTftpClientPort,
    /**
     * Device containing the SDI file.
     */
    DeviceRamDiskSdiDevice,
    /**
     * Path to the SDI file.
     */
    DeviceRamDiskSdiPath,
    /**
     * Length of the RAM disk image.
     */
    DeviceRamDiskRamDiskImageLength,
    /**
     * Exports the image as a CD.
     */
    DeviceRamDiskExportAsCd,
    /**
     * The TFTP transfer block size.
     */
    DeviceRamDiskTftpBlockSize,
    /**
     * The device type.
     */
    SetupDeviceType,
    /**
     * The application relative path.
     */
    SetupAppRelativePath,
    /**
     * The device relative path.
     */
    SetupRamDiskDeviceRelativePath,
    /**
     * Omit OS loader elements.
     */
    SetupOmitOsLoaderElements,
    /**
     * Recovery OS flag.
     */
    SetupRecoveryOs
}
