package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;

import javax.annotation.Nullable;

@Data
public class AccountProperties {
    private Boolean angryClient;
    @Nullable
    private Boolean showScamWarningDisabled;
    @Nullable
    private Boolean appHostingMessageDisabled;
    private Boolean hideGoogleAdWords;
    private Boolean googleActionUsed;
    private Boolean bonusOnFirstMobilePaymentActionUsed;
}
