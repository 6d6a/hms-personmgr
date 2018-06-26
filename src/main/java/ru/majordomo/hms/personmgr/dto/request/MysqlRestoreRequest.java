package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
public class MysqlRestoreRequest implements RestoreRequest {
    @NotEmpty(message = "Не указан id резервной копии")
    private String snapshotId;

    @NotEmpty(message = "Не указан id базы данных")
    private String databaseId;
}
