package ru.majordomo.hms.personmgr.dto;

public class ResourceCounter extends StatCounter {

    private String resourceId;

    private String name;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
