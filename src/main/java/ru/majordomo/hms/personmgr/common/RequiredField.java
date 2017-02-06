package ru.majordomo.hms.personmgr.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RequiredField {
    public static final Set<String> ACCOUNT_CREATE = new HashSet<>();

    static {
        ACCOUNT_CREATE.addAll(Arrays.asList(
                "plan",
                "emailAddresses",
                "name",
                "agreement"
        ));
    }
}
