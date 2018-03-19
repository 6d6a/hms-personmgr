package ru.majordomo.hms.personmgr.dto.revisium.rows;

import java.util.ArrayList;

public class SuspiciousRow extends ArrayList<String>
{
    public String getSuspiciousUrl() {
        return this.get(0);
    }

    public String getSuspiciousType() {
        return this.get(1);
    }
}
