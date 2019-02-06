package ru.majordomo.hms.personmgr.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RepositoryConstraintViolationException extends BaseException {

    public RepositoryConstraintViolationException() {}

    @Setter
    @Getter
    private List<FieldErrorResource> fieldErrors;
}
