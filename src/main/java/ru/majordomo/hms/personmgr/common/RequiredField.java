package ru.majordomo.hms.personmgr.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.APP_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.ENABLED_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEB_SITE_ID_KEY;

public class RequiredField {
    public static final Set<String> ACCOUNT_CREATE = new HashSet<>();

    public static final Set<String> ACCOUNT_SERVICE_CREATE = new HashSet<>();

    public static final Set<String> ACCOUNT_SERVICE_ENABLE = new HashSet<>();

    public static final Set<String> ACCOUNT_SEO_ORDER_CREATE = new HashSet<>();

    public static final Set<String> ACCOUNT_PASSWORD_CHANGE = new HashSet<>();

    public static final Set<String> ACCOUNT_PASSWORD_RECOVER = new HashSet<>();

    public static final Set<String> APP_INSTALL = new HashSet<>();

    public static final Set<String> APP_INSTALL_FULL = new HashSet<>();

    static {
        ACCOUNT_CREATE.addAll(Arrays.asList(
                "plan",
                "emailAddresses",
                "name",
                "agreement"
        ));

        ACCOUNT_SERVICE_CREATE.addAll(Collections.singletonList(
                "paymentServiceId"
        ));

        ACCOUNT_SERVICE_ENABLE.addAll(Collections.singletonList(
                ENABLED_KEY
        ));

        ACCOUNT_SEO_ORDER_CREATE.addAll(Arrays.asList(
                "domainName",
                "seoType"
        ));

        APP_INSTALL.addAll(Arrays.asList(
                WEB_SITE_ID_KEY,
                APP_ID_KEY
        ));

        APP_INSTALL_FULL.addAll(APP_INSTALL);

        APP_INSTALL_FULL.addAll(Arrays.asList(
                DATABASE_ID_KEY,
                DATABASE_USER_ID_KEY,
                DATABASE_USER_PASSWORD_KEY
        ));

        ACCOUNT_PASSWORD_CHANGE.addAll(Collections.singletonList(
                PASSWORD_KEY
        ));

        ACCOUNT_PASSWORD_RECOVER.addAll(Collections.singletonList(
                ACCOUNT_ID_KEY
        ));
    }
}
