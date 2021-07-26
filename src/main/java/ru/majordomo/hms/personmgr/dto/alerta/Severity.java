package ru.majordomo.hms.personmgr.dto.alerta;

/**
 * @see <a href="https://docs.alerta.io/en/latest/api/alert.html#alert-severities">https://docs.alerta.io/en/latest/api/alert.html#alert-severities</a>
 */
public enum Severity {
    security,
    critical,
    major,
    minor,
    warning,
    informational,
    debug,
    trace,
    indeterminate,
    cleared,
    normal,
    ok,
    unknown
}
