package ru.majordomo.hms.personmgr.validation.validator;

import ru.majordomo.hms.personmgr.validation.RelativePath;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RelativePathValidator implements ConstraintValidator<RelativePath, String>  {
    @Override
    public void initialize(RelativePath validPhone) {
    }

    @Override
    public boolean isValid(final String path, ConstraintValidatorContext constraintValidatorContext) {
        if (path == null) return true;

        Path path1 = Paths.get(path);

        return !path1.isAbsolute() && !path1.normalize().startsWith("..");
    }
}
