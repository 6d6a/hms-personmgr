package ru.majordomo.hms.personmgr.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static ru.majordomo.hms.personmgr.common.Constants.ENABLED_KEY;

public class RequiredField {
    public static final Set<String> ACCOUNT_CREATE = new HashSet<>();

    public static final Set<String> ACCOUNT_SERVICE_CREATE = new HashSet<>();

    public static final Set<String> ACCOUNT_SERVICE_ENABLE = new HashSet<>();

    public static final Set<String> ACCOUNT_SEO_ORDER_CREATE = new HashSet<>();

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
                "webSiteId",
                "seoType"
        ));
    }
}
