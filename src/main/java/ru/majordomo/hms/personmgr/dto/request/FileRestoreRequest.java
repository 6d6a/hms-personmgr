package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class FileRestoreRequest {
    String serverName;

    @NotEmpty(message = "Не указан id резервной копии")
    String snapshotId;

    @NotNull(message = "Не указан путь для восстановления в поле 'pathFrom'")
    @Pattern(
            regexp = "^((?!\\.)[\\.a-zA-Zа-яА-Я0-9ёЁ\\-_]*/?)+",
            message = "Относительный путь должен состоять из букв, цифр, символов '.', '-', '_' и начинаться с символа '/'"
    )
    String pathFrom;

    @Pattern(
            regexp = "^((?!\\.)[\\.a-zA-Zа-яА-Я0-9ёЁ\\-_]*/?)+",
            message = "Относительный путь должен состоять из букв, цифр, символов '.', '-', '_' и начинаться с символа '/'"
    )
    String pathTo;

    Boolean deleteExtraneous;
}
