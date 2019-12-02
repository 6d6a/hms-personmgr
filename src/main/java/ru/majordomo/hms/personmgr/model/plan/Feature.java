package ru.majordomo.hms.personmgr.model.plan;

import lombok.NonNull;
import org.apache.commons.lang.StringUtils;
import ru.majordomo.hms.personmgr.common.Constants;

import javax.annotation.Nullable;

//todo удалить forSomePlan, dailyPayment, oldId, вместо них использовать ServicePlan

public enum Feature {
    VIRTUAL_HOSTING_PLAN("", true),
    ADDITIONAL_SERVICE,
    DOMAIN_REGISTRATION,
    DOMAIN_RENEW,
    SEO,
    GOOGLE_3000,
    REVISIUM,
    SMS_NOTIFICATIONS(Constants.SMS_NOTIFICATIONS_29_RUB_SERVICE_ID, true),
    FTP_USER,
    DISK_QUOTA,
    ANTI_SPAM(Constants.ANTI_SPAM_SERVICE_ID, true),
    MAILBOX,
    SSL_CERTIFICATE,
    WEB_SITE,
    DATABASE,
    DATABASE_USER,
    BUSINESS_SERVICES,
    DOCUMENT_PACKAGE_ORDER,
    ADDITIONAL_QUOTA_5K(true),
    REDIRECT,
    LONG_LIFE_RESOURCE_ARCHIVE,
    ADVANCED_BACKUP,
    ADVANCED_BACKUP_INSTANT_ACCESS,
    /**
     *  разрешение создавать базы данных и пользователей баз данных даже если они запрещены тарифом
     */
    ALLOW_USE_DATABASES,
    DEDICATED_APP_SERVICE;


    @NonNull
    private final String oldId;
    private final boolean dailyPayment;     // для данной возможности доступны посуточные списания на основе коллекции AccountService
    private final boolean forSomePlan;      // требуется дополнительная проверка разрешена ли дополнительная услуга для определенного тарифа

    Feature() {
        oldId = "";
        dailyPayment = false;
        forSomePlan = false;
    }

    /**
     * @deprecated will delete
     * @param forSomePlan
     */
    @Deprecated
    Feature(boolean forSomePlan) {
        this.forSomePlan = forSomePlan;
        oldId = "";
        dailyPayment = false;
    }

    /**
     * @deprecated will delete
     * @param oldId
     * @param dailyPayment
     * @param forSomePlan
     */
    @Deprecated
    Feature(@NonNull String oldId, boolean dailyPayment, boolean forSomePlan) {
        this.oldId = oldId;
        this.dailyPayment = dailyPayment;
        this.forSomePlan = forSomePlan;
    }

    /**
     * @deprecated will delete, use ServicePlan
     * @param oldId
     * @param dailyPayment
     */
    Feature(@NonNull String oldId, boolean dailyPayment) {
        this.oldId = oldId;
        this.dailyPayment = dailyPayment;
        this.forSomePlan = false;
    }

    /**
     * @deprecated will delete, use ServicePlan
     * @return
     */
    @Deprecated
    public boolean isForSomePlan() {
        return forSomePlan;
    }

    /**
     * oldId - идентификатор экспортированный из старого билинга
     * PaymentService для услуг с посуточным списанием можно получить только по oldId;
     * @return oldId услуги или пустую строку
     * @deprecated will delete, use ServicePlan
     */
    @Deprecated
    public String getOldId() {
        return oldId;
    }

    /**
     * Проверка допускает услуга посуточные списания.
     * @return true если допускает.
     * @deprecated will delete, use ServicePlan
     */
    @Deprecated
    public boolean isDailyPayment() {
        return dailyPayment;
    }

    /**
     * Возвращает Feature по oldId
     * @param oldId ид из старой панели. Пригоден только для старых
     * @return Объект Feature или null - если нет подходящего идентификатора
     * @deprecated will delete, use ServicePlan
     */
    @Nullable
    @Deprecated
    public static Feature byOldId(String oldId) {
        return SMS_NOTIFICATIONS.oldId.equals(oldId) ? SMS_NOTIFICATIONS : (ANTI_SPAM.oldId.equals(oldId) ? ANTI_SPAM : null); // вызов в цикле сбивал бы с толку статический анализатор.
    }

    public boolean isOnlyOnePerAccount() {
        switch (this) {
            case VIRTUAL_HOSTING_PLAN:
            case SMS_NOTIFICATIONS:
            case ANTI_SPAM:
            case ADVANCED_BACKUP:
            case ADVANCED_BACKUP_INSTANT_ACCESS:
            case ADDITIONAL_QUOTA_5K:
                return true;
            default:
                return false;
        }
    }

    public boolean canUserDelete() {
        switch (this) {
            case VIRTUAL_HOSTING_PLAN:
                return false;
            default:
                return true;
        }
    }
}
