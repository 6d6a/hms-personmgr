package ru.majordomo.hms.personmgr.dto.revisium.rows;

import java.util.ArrayList;

public class ResourceExternalRow extends ArrayList<String>
{
    public String getResourceExternalUrl() {
        return this.get(0);
    }

    public String getResourceExternalType() {
        return this.get(1);
    }
}
