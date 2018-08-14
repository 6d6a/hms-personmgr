package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import ru.majordomo.hms.personmgr.validation.RelativePath;

import javax.validation.constraints.NotNull;

@Data
public class FileRestoreRequest implements RestoreRequest {

    private String serverName;

    @NotEmpty(message = "Не указан id резервной копии")
    private String snapshotId;

    @NotNull(message = "Не указан путь для восстановления в поле 'pathFrom'")
    @RelativePath
    private String pathFrom;

    @RelativePath
    private String pathTo;

    private Boolean deleteExtraneous;
}
