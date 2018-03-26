package ru.majordomo.hms.personmgr.dto.revisium.rows;

import java.util.ArrayList;

public class RedirectRow extends ArrayList<String>
{
    public String getRedirectType() {
        return this.get(0);
    }
}
