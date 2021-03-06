package ru.majordomo.hms.personmgr.model;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.data.annotation.Id;

import ru.majordomo.hms.personmgr.common.Views;

/**
 * Base class for document classes.
 */
public class BaseModel {
    @JsonView(Views.Public.class)
    @Id
    private String id;

    /**
     * Returns the identifier of the document.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void unSetId() {
        this.id = null;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (this.id == null || obj == null || !(this.getClass().equals(obj.getClass()))) {
            return false;
        }

        BaseModel that = (BaseModel) obj;

        return this.id.equals(that.getId());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "BaseModel{" +
                "id='" + id + '\'' +
                '}';
    }
}
