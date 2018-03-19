package ru.majordomo.hms.personmgr.dto.revisium.rows;

import java.util.ArrayList;

public class BlacklistUrlRow extends ArrayList<String>
{
    public String getBlacklistUrl() {
        return this.get(0);
    }

    public String getBlacklistType() {
        return this.get(1);
    }
}
