package ru.majordomo.hms.personmgr.common.message;

import ru.majordomo.hms.personmgr.common.DBType;

/**
 * DatabaseCreateMessageParams
 */
public class DatabaseCreateMessageParams extends ServiceMessageParams {
    private DBType dbType;

    public DBType getDbType() {
        return dbType;
    }

    public void setDbType(DBType dbType) {
        this.dbType = dbType;
    }
}
