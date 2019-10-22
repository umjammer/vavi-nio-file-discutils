/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.util.Arrays;


/**
 * WellKnownSidType.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/13 nsano initial version <br>
 */
public enum WellKnownSidType {
    NullSid,
    WorldSid,
    LocalSid,
    CreatorOwnerSid,
    CreatorGroupSid,
    CreatorOwnerServerSid,
    CreatorGroupServerSid,
    NTAuthoritySid,
    DialupSid,
    NetworkSid,
    BatchSid,
    InteractiveSid,
    ServiceSid,
    AnonymousSid,
    ProxySid,
    EnterpriseControllersSid,
    SelfSid,
    AuthenticatedUserSid,
    RestrictedCodeSid,
    TerminalServerSid,
    RemoteLogonIdSid,
    LogonIdsSid,
    LocalSystemSid,
    LocalServiceSid,
    NetworkServiceSid,
    BuiltinDomainSid,
    BuiltinAdministratorsSid,
    BuiltinUsersSid,
    BuiltinGuestsSid,
    BuiltinPowerUsersSid,
    BuiltinAccountOperatorsSid,
    BuiltinSystemOperatorsSid,
    BuiltinPrintOperatorsSid,
    BuiltinBackupOperatorsSid,
    BuiltinReplicatorSid,
    BuiltinPreWindows2000CompatibleAccessSid,
    BuiltinRemoteDesktopUsersSid,
    BuiltinNetworkConfigurationOperatorsSid,
    AccountAdministratorSid,
    AccountGuestSid,
    AccountKrbtgtSid,
    AccountDomainAdminsSid,
    AccountDomainUsersSid,
    AccountDomainGuestsSid,
    AccountComputersSid,
    AccountControllersSid,
    AccountCertAdminsSid,
    AccountSchemaAdminsSid,
    AccountEnterpriseAdminsSid,
    AccountPolicyAdminsSid,
    AccountRasAndIasServersSid,
    NtlmAuthenticationSid,
    DigestAuthenticationSid,
    SChannelAuthenticationSid,
    ThisOrganizationSid,
    OtherOrganizationSid,
    BuiltinIncomingForestTrustBuildersSid,
    BuiltinPerformanceMonitoringUsersSid,
    BuiltinPerformanceLoggingUsersSid,
    BuiltinAuthorizationAccessSid,
    WinBuiltinTerminalServerLicenseServersSid,
    MaxDefined,
    WinBuiltinDCOMUsersSid,
    WinBuiltinIUsersSid,
    WinIUserSid,
    WinBuiltinCryptoOperatorsSid,
    WinUntrustedLabelSid,
    WinLowLabelSid,
    WinMediumLabelSid,
    WinHighLabelSid,
    WinSystemLabelSid,
    WinWriteRestrictedCodeSid,
    WinCreatorOwnerRightsSid,
    WinCacheablePrincipalsGroupSid,
    WinNonCacheablePrincipalsGroupSid,
    WinEnterpriseReadonlyControllersSid,
    WinAccountReadonlyControllersSid,
    WinBuiltinEventLogReadersGroup,
    WinNewEnterpriseReadonlyControllersSid,
    WinBuiltinCertSvcDComAccessGroup,
    WinMediumPlusLabelSid,
    WinLocalLogonSid,
    WinConsoleLogonSid,
    WinThisOrganizationCertificateSid,
    WinApplicationPackageAuthoritySid,
    WinBuiltinAnyPackageSid,
    WinCapabilityInternetClientSid,
    WinCapabilityInternetClientServerSid,
    WinCapabilityPrivateNetworkClientServerSid,
    WinCapabilityPicturesLibrarySid,
    WinCapabilityVideosLibrarySid,
    WinCapabilityMusicLibrarySid,
    WinCapabilityDocumentsLibrarySid,
    WinCapabilitySharedUserCertificatesSid,
    WinCapabilityEnterpriseAuthenticationSid,
    WinCapabilityRemovableStorageSid;

    public static WellKnownSidType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> value == v.ordinal()).findFirst().get();
    }
}

/* */
