package ru.majordomo.hms.personmgr.common;

import ru.majordomo.hms.rc.staff.resources.template.ApplicationServer;

import javax.annotation.Nullable;

public enum Language {
    PHP, PERL, PYTHON, JAVASCRIPT,
    /**
     * Статические файлы
     */
    STATIC;

    public boolean equivalent(@Nullable ApplicationServer.Language language) {
        return language != null && name().equals(language.name());
    }
}
