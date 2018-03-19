package ru.majordomo.hms.personmgr.dto.revisium.rows;

import java.util.ArrayList;

public class ResourceErrorRow extends ArrayList<String>
{
    public String getResourceErrorUrl() {
        return this.get(0);
    }

    public String getResourceErrorCode() {
        return this.get(1);
    }
}
