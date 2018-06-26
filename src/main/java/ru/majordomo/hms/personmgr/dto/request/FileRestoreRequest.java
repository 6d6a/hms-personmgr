package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class FileRestoreRequest implements RestoreRequest {

    private String serverName;

    @NotEmpty(message = "Не указан id резервной копии")
    private String snapshotId;

    @NotNull(message = "Не указан путь для восстановления в поле 'pathFrom'")
    @Pattern(
            regexp = "^((?!\\.)[\\.a-zA-Zа-яА-Я0-9ёЁ\\-_]*/?)+",
            message = "Должен быть относительным путём"
    )
    private String pathFrom;

    @Pattern(
            regexp = "^((?!\\.)[\\.a-zA-Zа-яА-Я0-9ёЁ\\-_]*/?)+",
            message = "Должен быть относительным путём"
    )
    private String pathTo;

    private Boolean deleteExtraneous;
}
