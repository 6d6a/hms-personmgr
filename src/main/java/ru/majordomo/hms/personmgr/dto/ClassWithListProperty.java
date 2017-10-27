package ru.majordomo.hms.personmgr.dto;

import java.util.ArrayList;
import java.util.List;

public class ClassWithListProperty {
    private List<String> ids;//= new ArrayList<>();

    public List<String> getIds() {
        return this.ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
