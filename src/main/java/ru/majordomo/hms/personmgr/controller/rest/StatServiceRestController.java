package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;


@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping({"/pm/stat"})
public class StatServiceRestController {
    private final MongoOperations mongoOperations;

    public StatServiceRestController(
            MongoOperations mongoOperations
    ) {
        this.mongoOperations = mongoOperations;
    }

    @RequestMapping(value = "/count/all-accounts", method = RequestMethod.GET)
    public ResponseEntity<Long> getAccountCount(
    ) {
        return new ResponseEntity<>(mongoOperations.count(query(where("")), PersonalAccount.class), HttpStatus.OK);
    }

    @RequestMapping(value = "/registration-by-date", method = RequestMethod.GET)
    public ResponseEntity<Long> getAccountCount(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(value = "date") LocalDate date
    ) {
        return new ResponseEntity<>(
                mongoOperations.count(
                        query(where("created")
                                .gt(LocalDateTime.of(date, LocalTime.MIN))
                                .lt(LocalDateTime.of(date, LocalTime.MAX))),
                        PersonalAccount.class
                ), HttpStatus.OK);
    }
}
