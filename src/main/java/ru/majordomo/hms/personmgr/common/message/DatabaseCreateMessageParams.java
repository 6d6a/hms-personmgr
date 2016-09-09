package ru.majordomo.hms.personmgr.common.message;

import ru.majordomo.hms.personmgr.common.DBType;

/**
 * DatabaseCreateMessageParams
 */
public class DatabaseCreateMessageParams extends ServiceMessageParams {
    private String id;
    private DBType dbType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DBType getDbType() {
        return dbType;
    }

    public void setDbType(DBType dbType) {
        this.dbType = dbType;
    }

    @Override
    public String toString() {
        return "DatabaseCreateMessageParams{" +
                "id='" + id + '\'' +
                ", dbType=" + dbType +
                "} " + super.toString();
    }
}
